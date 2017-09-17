package com.splitcriteria.timecard;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

/**
 * Created by Deuce on 9/14/17.
 */

public class ProjectReceiverClockInOut extends BroadcastReceiver {

    static final int NOTIFICATION_CLOCK_OUT_ID = 1;

    private static final String ACTION_ROOT = "com.splitcriteria.timecard.action.";
    static final String ACTION_CLOCK_IN = ACTION_ROOT + "CLOCK_IN";
    static final String ACTION_CLOCK_OUT = ACTION_ROOT + "CLOCK_OUT";
    static final String ACTION_CLOCK_TOGGLE = ACTION_ROOT + "CLOCK_TOGGLE";

    public static Intent getClockInIntent(Context context, String projectName) {
        return getClockActionIntent(context, projectName, ACTION_CLOCK_IN);
    }

    public static Intent getClockOutIntent(Context context, String projectName) {
        return getClockActionIntent(context, projectName, ACTION_CLOCK_OUT);
    }

    public static Intent getClockToggleIntent(Context context, String projectName) {
        return getClockActionIntent(context, projectName, ACTION_CLOCK_TOGGLE);
    }

    public static Intent getClockActionIntent(Context context, String projectName, String action) {
        Intent intent = new Intent(context, ProjectReceiverClockInOut.class);
        // Set the data to the project name (instead of using an Extra) because we need the
        // Intents to be distinct from one another. Extras are not considered during Intent
        // equality comparison, but the data is.
        intent.setData(projectName != null ? Uri.parse(projectName) : null);
        intent.setAction(action);
        return intent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String projectName = intent.getData().toString();
            String action = intent.getAction();
            String userMessage = null;
            String clockInMessage = context.getString(R.string.broadcast_clock_in,
                                                      context.getString(R.string.app_name),
                                                      projectName);
            String clockOutMessage = context.getString(R.string.broadcast_clock_out,
                                                       context.getString(R.string.app_name),
                                                       projectName);
            ProjectData pd = new ProjectData(context, MainActivity.PROJECTS_DB_NAME);
            boolean isClockedIn = pd.isClockedIn(projectName);
            if (action != null) {
                if (action.equals(ACTION_CLOCK_OUT) ||
                        (action.equals(ACTION_CLOCK_TOGGLE) && isClockedIn)) {
                    pd.clockOut(projectName);
                    userMessage = clockOutMessage;
                    removeNotification(context, projectName);
                } else if (action.equals(ACTION_CLOCK_IN) ||
                        (action.equals(ACTION_CLOCK_TOGGLE) && !isClockedIn)) {
                    pd.clockIn(projectName);
                    userMessage = clockInMessage;
                    postNotification(context, projectName);
                }
            }
            pd.close();
            if (userMessage == null) {
                userMessage = context.getString(R.string.broadcast_error,
                                                context.getString(R.string.app_name));
            }
            Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void removeNotification(Context context, String projectName) {
        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(projectName, NOTIFICATION_CLOCK_OUT_ID);
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
        Intent intent = getClockOutIntent(context, projectName);
        PendingIntent projectPendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        builder.setContentIntent(projectPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(projectName, NOTIFICATION_CLOCK_OUT_ID, builder.build());
    }
}
