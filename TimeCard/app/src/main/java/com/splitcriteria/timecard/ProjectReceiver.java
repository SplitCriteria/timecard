package com.splitcriteria.timecard;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A receiver which clocks in/out projects and controls posting and dismissing
 * notifications which allow the user to continue controlling their projects
 * Use the ProjectReceiver.IntentBuilder to build an Intent or PendingIntent.
 */

public class ProjectReceiver extends BroadcastReceiver {

    static final int NOTIFICATION_CLOCK_OUT_ID = 1;
    static final int NOTIFICATION_STICKY_ID = 2;

    private static final String ACTION_ROOT = "com.splitcriteria.timecard.action.";
    static final String ACTION_CLOCK_IN = ACTION_ROOT + "CLOCK_IN";
    static final String ACTION_CLOCK_OUT = ACTION_ROOT + "CLOCK_OUT";
    static final String ACTION_CLOCK_TOGGLE = ACTION_ROOT + "CLOCK_TOGGLE";
    static final String ACTION_POST_STICKY = ACTION_ROOT + "POST_STICKY";
    static final String ACTION_DISMISS = ACTION_ROOT + "DISMISS";
    static final String ACTION_DISMISS_STICKY = ACTION_ROOT + "DISMISS_STICKY";

    static final String KEY_FROM_STICKY = "from_sticky";
    static final String KEY_SUPPRESS_NOTIFICATION = "suppress_notification";
    static final String KEY_SUPPRESS_TOAST = "suppress_toast";
    static final String KEY_EXTRA_DATA = "extra_data";

    /**
     * Convenience class for building an Intent or PendingIntent which contains
     * an action (e.g. ACTION_CLOCK_IN, ACTION_POST_STICKY) and other amplifying
     * information (e.g. flags to suppress a Toast notification, include Extra Data).
     */
    static class IntentBuilder {
        private Intent mIntent;
        private Context mContext;
        IntentBuilder(Context context, String projectName) {
            mContext = context;
            mIntent = new Intent(context, ProjectReceiver.class);
            // Set the data to the project name (instead of using an Extra) because we need the
            // Intents to be distinct from one another. Extras are not considered during Intent
            // equality comparison, but the data is.
            mIntent.setData(projectName != null ? Uri.parse(projectName) : null);
            // Set the default arguments
            setSuppressToast(false);
            setSuppressNotification(false);
        }
        IntentBuilder setAction(String action) {
            mIntent.setAction(action);
            return this;
        }
        IntentBuilder setSuppressToast(boolean suppressToast) {
            mIntent.putExtra(KEY_SUPPRESS_TOAST, suppressToast);
            return this;
        }
        IntentBuilder setSuppressNotification(boolean suppressNotification) {
            mIntent.putExtra(KEY_SUPPRESS_NOTIFICATION, suppressNotification);
            return this;
        }
        IntentBuilder setFromSticky(boolean fromSticky) {
            mIntent.putExtra(KEY_FROM_STICKY, fromSticky);
            return this;
        }
        IntentBuilder setExtraData(String extraData) {
            mIntent.putExtra(KEY_EXTRA_DATA, extraData);
            return this;
        }
        Intent build() {
            return mIntent;
        }
        PendingIntent buildPendingIntent() {
            return PendingIntent.getBroadcast(mContext, 0, mIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the project name and the action from the Intent
        String projectName = intent.getData().toString();
        String action = intent.getAction();
        boolean suppressToast = intent.getBooleanExtra(KEY_SUPPRESS_TOAST, false);
        // Resolve the ACTION_TOGGLE to either ACTION_CLOCK_IN or ACTION_CLOCK_OUT
        if (action.equals(ACTION_CLOCK_TOGGLE)) {
            action = getToggleAction(context, projectName);
        }
        String databaseFilename = context.getString(R.string.default_database_filename);
        // Handle the intent actions
        switch (action) {
            case ACTION_CLOCK_IN: {
                // Get any extra data associated with clocking in
                String extraData = intent.getStringExtra(KEY_EXTRA_DATA);
                // If this intent was from a remote input (i.e. a notification) then
                // override the extraData with the RemoteInput data
                String remoteData = getRemoteData(intent);
                boolean hasRemoteData = remoteData != null;
                // Clock the project in
                ProjectData pd = new ProjectData(context, databaseFilename);
                pd.clockIn(projectName, hasRemoteData ? remoteData : extraData);
                ProjectData.Metadata metadata = pd.getProjectMetadata(projectName);
                pd.close(context);
                // If no extra data is given, check for default data
                if (metadata.usesExtraData && TextUtils.isEmpty(extraData)) {
                    extraData = metadata.defaultExtraData;
                }
                if (!suppressToast && !hasRemoteData) {
                    Toast.makeText(
                            context,
                            context.getString(metadata.noDuration ?
                                            R.string.broadcast_clock_in_instant :
                                            R.string.broadcast_clock_in,
                                    context.getString(R.string.app_name),
                                    projectName),
                            Toast.LENGTH_LONG).show();
                }
                // If the data came from a notification, then reply to it
                if (intent.getBooleanExtra(KEY_FROM_STICKY, false)) {
                    postStickyNotification(context, projectName,
                            getStickyNotificationMessage(context, metadata,
                                    hasRemoteData ? remoteData : extraData, -1));
                } else if (!metadata.noDuration
                        && !intent.getBooleanExtra(KEY_SUPPRESS_NOTIFICATION, false)) {
                    // Post a clock out notification if this project is not "instant"
                    postNotification(context, projectName);
                }
                break;
            }
            case ACTION_CLOCK_OUT: {
                ProjectData pd = new ProjectData(context, databaseFilename);
                int duration = pd.clockOut(projectName);
                ProjectData.Metadata metadata = pd.getProjectMetadata(projectName);
                pd.close(context);
                if (intent.getBooleanExtra(KEY_FROM_STICKY, false)) {
                    postStickyNotification(context, projectName,
                            getStickyNotificationMessage(context, metadata, null, duration));
                } else {
                    dismissNotification(context, projectName);
                }
                if (!suppressToast) {
                    Toast.makeText(
                            context,
                            context.getString(R.string.broadcast_clock_out,
                                    context.getString(R.string.app_name),
                                    projectName),
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
            case ACTION_DISMISS:
                dismissNotification(context, projectName);
                break;
            case ACTION_POST_STICKY: {
                ProjectData projectData = new ProjectData(context, databaseFilename);
                ProjectData.Metadata metadata = projectData.getProjectMetadata(projectName);
                projectData.close(context);
                int messageID = metadata.noDuration ?
                        R.string.notification_sticky_mark_time_instructions :
                        metadata.currentTimecard == -1 ?
                                R.string.notification_sticky_clock_in_instructions :
                                R.string.notification_sticky_clock_out_instructions;
                postStickyNotification(context, projectName, context.getString(messageID));
                break;
            }
            case ACTION_DISMISS_STICKY:
                dismissStickyNotification(context, projectName);
                break;
        }
    }

    private String getToggleAction(Context context, String projectName) {
        ProjectData projectData = new ProjectData(
                context, context.getString(R.string.default_database_filename));
        String action = projectData.isClockedIn(projectName) ? ACTION_CLOCK_OUT : ACTION_CLOCK_IN;
        projectData.close(context);
        return action;
    }

    private String getRemoteData(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            CharSequence remoteData = remoteInput.getCharSequence(KEY_EXTRA_DATA, null);
            if (remoteData != null) {
                return remoteData.toString();
            }
        }
        return null;
    }

    private String getStickyNotificationMessage(Context context,
                                                ProjectData.Metadata metadata,
                                                String extraData,
                                                int duration) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = dateFormat.format(new Date());
        if (metadata.noDuration) {
            if (TextUtils.isEmpty(extraData)) {
                return context.getString(R.string.notification_sticky_marked_time, time);
            } else {
                return context.getString(R.string.notification_sticky_marked_time_with_data,
                                         extraData, time);
            }
        } else {
            if (metadata.currentTimecard == -1) {
                // Calculate duration of most recent clock in
                int days = duration / 86400;
                int hours = (duration % 86400) / 3600;
                int minutes = (duration % 3600) / 60;
                int seconds = duration % 60;
                String durationMsg;
                if (days > 0) {
                    durationMsg = context.getString(R.string.time_days,
                                                    days, hours, minutes, seconds);
                } else if (hours > 0) {
                    durationMsg = context.getString(R.string.time_hours, hours, minutes, seconds);
                } else if (minutes > 0) {
                    durationMsg = context.getString(R.string.time_minutes, minutes, seconds);
                } else {
                    durationMsg = context.getString(R.string.time_seconds, seconds);
                }
                return context.getString(R.string.notification_sticky_clocked_out,
                                         durationMsg, time);
            } else {
                if (TextUtils.isEmpty(extraData)) {
                    return context.getString(R.string.notification_sticky_clocked_in, time);
                } else {
                    return context.getString(R.string.notification_sticky_clocked_in_with_data,
                                             extraData, time);
                }
            }
        }
    }

    private void dismissNotification(Context context, String projectName) {
        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(projectName, NOTIFICATION_CLOCK_OUT_ID);
    }

    private void dismissStickyNotification(Context context, String projectName) {
        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(projectName, NOTIFICATION_STICKY_ID);
    }

    private void postNotification(Context context, String projectName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_clock_out)
                .setContentTitle(
                        context.getString(R.string.notification_title_clock_out, projectName))
                .setContentText(
                        context.getString(R.string.notification_text_clock_out, projectName))
                .setChannel(projectName)
                .setAutoCancel(true)
                .setOngoing(true);

        // Build an intent to clock out the project using a broadcast receiver
        PendingIntent projectPendingIntent =
                new IntentBuilder(context, projectName)
                        .setAction(ACTION_CLOCK_OUT)
                        .buildPendingIntent();

        builder.setContentIntent(projectPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(projectName, NOTIFICATION_CLOCK_OUT_ID, builder.build());
    }

    private void postStickyNotification(Context context, String projectName, String message) {
        // Configure the remote input for the notification
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_EXTRA_DATA)
                .setLabel(context.getString(R.string.notification_extra_data_instruction))
                .build();

        ProjectData projectData = new ProjectData(
                context, context.getString(R.string.default_database_filename));
        ProjectData.Metadata metadata = projectData.getProjectMetadata(projectName);
        projectData.close(context);
        boolean isClockedIn = metadata.currentTimecard != -1;

        // Add a Clock In/Out or Mark Time action
        int stringID;
        if (isClockedIn) {
            stringID = R.string.clock_out;
        } else {
            stringID = metadata.noDuration ? R.string.clock_in_instant : R.string.clock_in;
        }
        NotificationCompat.Action clockInOut = new NotificationCompat.Action.Builder(
                0, context.getString(stringID),
                new ProjectReceiver.IntentBuilder(context, projectName)
                        .setAction(ACTION_CLOCK_TOGGLE)
                        .setFromSticky(true)
                        .setSuppressNotification(true)
                        .buildPendingIntent())
                .build();

        // Create the Enter Extra Data notification action
        NotificationCompat.Action getExtraData = new NotificationCompat.Action.Builder(
                R.drawable.ic_mark_notification,
                context.getString(R.string.notification_extra_data),
                new ProjectReceiver.IntentBuilder(context, projectName)
                        .setAction(ProjectReceiver.ACTION_CLOCK_IN)
                        .setFromSticky(true)
                        .buildPendingIntent())
                .addRemoteInput(remoteInput)
                .build();

        // Create the Dismiss notification action
        NotificationCompat.Action dismiss = new NotificationCompat.Action.Builder(
                0, context.getString(R.string.notification_action_dismiss),
                new ProjectReceiver.IntentBuilder(context, projectName)
                        .setFromSticky(true)
                        .setAction(ProjectReceiver.ACTION_DISMISS_STICKY)
                        .buildPendingIntent())
                .build();

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_mark_notification)
                .setContentTitle(
                        context.getString(R.string.notification_title_clock_out, projectName))
                .setContentText(message)
                .setChannel(projectName)
                .addAction(clockInOut);
        if (!isClockedIn) {
            builder.addAction(getExtraData);
        }
        builder.addAction(dismiss).setAutoCancel(false).setOngoing(true);

        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(projectName, NOTIFICATION_STICKY_ID, builder.build());
    }
}
