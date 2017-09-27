package com.splitcriteria.timecard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
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

        // This activity should only be started by the Android system which contains
        // extras containing the app widget ID
        assert extras != null;

        mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                     AppWidgetManager.INVALID_APPWIDGET_ID);
        // Set the initial result to canceled in case the user backs out
        Intent resultIntent = new Intent();
        resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultIntent);

        // Set up the spinner with project names
        ProjectData pd = new ProjectData(this, getString(R.string.database_filename));
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
            // Make sure a project has been selected, if not then notify the user
            if (mSelectedProject == null) {
                Toast.makeText(this, R.string.widget_no_project_selected, Toast.LENGTH_LONG).show();
                return;
            }
            // Set up the widget
            ProjectWidgetProvider.setupWidget(this, mSelectedProject, mAppWidgetId);
            // Set the result to OK and finish
            setResultAndFinish(RESULT_OK);
        }
    }

    private void setResultAndFinish(int resultCode) {
        Intent intent = new Intent();
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(resultCode, intent);
        finish();
    }
}
