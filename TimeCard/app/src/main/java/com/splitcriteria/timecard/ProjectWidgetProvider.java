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

            // Get the project name
            String projectName = appWidgetManager
                    .getAppWidgetOptions(id)
                    .getString(Intent.EXTRA_TEXT);

            // Create a broadcast intent to clock in/out the widget
            Intent intent = new Intent(context, ProjectReceiverClockInOut.class);
            intent.setAction(ProjectActivity.ACTION_CLOCK_TOGGLE);
            intent.putExtra(Intent.EXTRA_TEXT, projectName);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Set up the widget view
            RemoteViews views = new RemoteViews(context.getPackageName(),
                                                R.layout.project_clock_in_out_widget);
            views.setTextViewText(R.id.name, projectName);
            views.setOnClickPendingIntent(R.id.name, pendingIntent);

            // Update the widget
            appWidgetManager.updateAppWidget(id, views);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }
}
