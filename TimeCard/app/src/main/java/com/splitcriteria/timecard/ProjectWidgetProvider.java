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
 * Timecard - Allows users to easily track time-based data for analysis.
 * Copyright (C) 2017  Nicholas Johnson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Timecard - Copyright (C) 2017  Nicholas Johnson
 *
 * This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
 * This is free software, and you are welcome to redistribute it
 * under certain conditions; type `show c' for details.
 *
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
