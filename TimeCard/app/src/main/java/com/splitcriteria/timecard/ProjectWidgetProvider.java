package com.splitcriteria.timecard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

/**
 * Created by Deuce on 9/14/17.
 */

public class ProjectWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {

            // Get the project name from the widget's options Bundle
            String projectName = appWidgetManager
                    .getAppWidgetOptions(id)
                    .getString(Intent.EXTRA_TEXT);

            // If there is no project name, then do no proceed
            assert projectName != null;

            // Set up the widget
            setupWidget(context, projectName, id);
        }
    }

    static void setupWidget(Context context, String projectName, int id) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // Create a broadcast intent to clock in/out the widget
        Intent intent = ProjectReceiverClockInOut.getClockToggleIntent(context, projectName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
