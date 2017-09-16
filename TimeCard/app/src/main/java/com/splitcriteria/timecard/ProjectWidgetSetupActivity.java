package com.splitcriteria.timecard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Created by Deuce on 9/14/17.
 */

public class ProjectWidgetSetupActivity extends AppCompatActivity implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private int mAppWidgetId;
    private String mSelectedProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project_widget_setup);
        setResult(RESULT_CANCELED, null);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                         AppWidgetManager.INVALID_APPWIDGET_ID);
            // Set the initial result to canceled in case the user backs out
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_CANCELED, resultIntent);
        }

        // Set up the spinner with project names
        ProjectData pd = new ProjectData(this, MainActivity.PROJECTS_DB_NAME);
        Spinner projectSpinner = (Spinner)findViewById(R.id.projects);
        projectSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.project_name,
                                                      pd.getProjectNames(false)));
        projectSpinner.setOnItemSelectedListener(this);
        pd.close();

        // Set up the on click listener to create the widget
        findViewById(R.id.config).setOnClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        mSelectedProject = (String)adapterView.getItemAtPosition(pos);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        mSelectedProject = null;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.config) {
            setupWidget();
            setResultAndFinish(RESULT_OK);
        }
    }

    private void setupWidget() {
        if (mSelectedProject == null) {
            Toast.makeText(this, R.string.widget_no_project_selected, Toast.LENGTH_LONG).show();
            return;
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        // Create a broadcast intent to clock in/out the widget
        Intent intent = ProjectReceiverClockInOut.getClockToggleIntent(this, mSelectedProject);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Set up the widget
        RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(),
                                            R.layout.project_clock_in_out_widget);
        views.setOnClickPendingIntent(R.id.name, pendingIntent);
        views.setTextViewText(R.id.name, mSelectedProject);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        // Add the project name to the options
        Bundle options = new Bundle();
        options.putString(Intent.EXTRA_TEXT, mSelectedProject);
        appWidgetManager.updateAppWidgetOptions(mAppWidgetId, options);
    }

    private void setResultAndFinish(int resultCode) {
        Intent intent = new Intent();
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(resultCode, intent);
        finish();
    }
}
