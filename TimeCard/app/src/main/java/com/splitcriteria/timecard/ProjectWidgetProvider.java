package com.splitcriteria.timecard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RemoteViews;

/**
 * Controls the update of the app's widgets. When clicked, the widget will toggle the
 * clock in/out of a project.
 */

public class ProjectWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {

            // Get the project name from the widget's options Bundle
            String projectName = appWidgetManager
                    .getAppWidgetOptions(id)
                    .getString(Intent.EXTRA_TEXT);

            if (TextUtils.isEmpty(projectName)) {
                return;
            }

            // Set up the widget
            setupWidget(context, projectName, id);
        }
    }

    static void setupWidget(Context context, String projectName, int id) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // Get any default extra data
        ProjectData projectData = new ProjectData(
                context, context.getString(R.string.default_database_filename));
        ProjectData.Metadata metadata = projectData.getProjectMetadata(projectName);
        projectData.close(context);
        String extraData = metadata.usesExtraData ? metadata.defaultExtraData : null;

        // Create a broadcast intent to clock in/out the widget
        PendingIntent pendingIntent = new ProjectReceiver.IntentBuilder(context, projectName)
                .setAction(ProjectReceiver.ACTION_CLOCK_TOGGLE)
                .setExtraData(extraData)
                .buildPendingIntent();

        // Set up the widget views
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.project_clock_in_out_widget);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        views.setTextViewText(R.id.name, projectName);
        appWidgetManager.updateAppWidget(id, views);

        // Add the project name to an options Bundle
        Bundle options = new Bundle();
        options.putString(Intent.EXTRA_TEXT, projectName);
        appWidgetManager.updateAppWidgetOptions(id, options);
    }
}
