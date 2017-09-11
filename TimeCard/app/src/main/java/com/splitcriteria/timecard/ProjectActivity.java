package com.splitcriteria.timecard;

import android.content.Intent;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ProjectActivity extends AppCompatActivity implements
        View.OnClickListener {

    private String mProjectName;
    private ProjectData mProjectData;
    private TextView mProjectTime;
    private Button mClockInOutButton;
    private boolean mIsClockedIn;
    private Handler mTimeUpdater;

    // Constants for second per X
    private static final int SEC_PER_DAY = 86400;
    private static final int SEC_PER_HOUR = 3600;
    private static final int SEC_PER_MIN = 60;

    private static final long TIME_UPDATE_DELAY = 1000; // 1 second

    private Runnable mUpdateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            refreshProjectTime();
            if (mIsClockedIn) {
                mTimeUpdater.postDelayed(this, TIME_UPDATE_DELAY);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        // Get the project name from the Intent
        mProjectName = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        // Set the title to the project name
        setTitle(mProjectName);
        // Get a reference to the Clock In/Out button
        mClockInOutButton = (Button)findViewById(R.id.clock_in_out);
        mClockInOutButton.setOnClickListener(this);
        // Get a reference to the Time
        mProjectTime = (TextView)findViewById(R.id.time);
        // Get a reference to the project data
        mProjectData = new ProjectData(this, MainActivity.PROJECTS_DB_NAME);
        mIsClockedIn = mProjectData.isClockedIn(mProjectName);
        // Set the text for the clock in/out button.
        refreshClockInOutButton();
        // Set the project time
        refreshProjectTime();
        // Set up the time updater
        mTimeUpdater = new Handler();
        mTimeUpdater.post(mUpdateTimeRunnable);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.clock_in_out) {
            if (mIsClockedIn) {
                mProjectData.clockOut(mProjectName);
                Snackbar.make(view, R.string.project_clocked_out, Snackbar.LENGTH_SHORT).show();
                // Remove the time updater
                mTimeUpdater.removeCallbacks(mUpdateTimeRunnable);
            } else {
                mProjectData.clockIn(mProjectName);
                Snackbar.make(view, R.string.project_clocked_in, Snackbar.LENGTH_SHORT).show();
                // Add the time updater
                mTimeUpdater.post(mUpdateTimeRunnable);
            }
            // Invert the clocked in flag
            mIsClockedIn = !mIsClockedIn;
            // Refresh the clock in/out button text
            refreshClockInOutButton();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.project, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        // Remove the time updater
        mTimeUpdater.removeCallbacks(mUpdateTimeRunnable);
        // Close the project data
        mProjectData.close();
        super.onDestroy();
    }

    private void refreshClockInOutButton() {
        // If the project is clocked in, then the text should say "Clock Out" and vice versa
        mClockInOutButton.setText(mIsClockedIn ? R.string.clock_out : R.string.clock_in);
    }

    private void refreshProjectTime() {
        int projectTime = mProjectData.getProjectTime(mProjectName);
        String timeText;
        int days = projectTime / SEC_PER_DAY;
        int hours = (projectTime % SEC_PER_DAY) / SEC_PER_HOUR;
        int minutes = (projectTime % SEC_PER_HOUR) / SEC_PER_MIN;
        int seconds = projectTime % SEC_PER_MIN;
        if (projectTime == -1) {
            timeText = getResources().getString(R.string.time_none);
        } else if (days > 0) {
            timeText = getResources().getString(
                    R.string.time_with_days, days, hours, minutes, seconds);
        } else {
            timeText = getResources().getString(
                    R.string.time_without_days, hours, minutes, seconds);
        }
        mProjectTime.setText(timeText);
    }
}
