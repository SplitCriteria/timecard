package com.splitcriteria.timecard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ProjectActivity extends AppCompatActivity implements
        View.OnClickListener,
        ResultFragment.OnResultListener {

    private String mProjectName;
    private TextView mProjectTime;
    private Button mClockInOutButton;
    private Handler mTimeUpdater;

    // Constants for second per X
    private static final int SEC_PER_DAY = 86400;
    private static final int SEC_PER_HOUR = 3600;
    private static final int SEC_PER_MIN = 60;

    private static final long TIME_UPDATE_DELAY = 1000; // 1 second

    private static final int REQUEST_CODE_CREATE_DOCUMENT = 1;

    private static final String KEY_PROJECT_NAME = "com.splitcriteria.timecard.project_name";

    private static final String TAG_DIALOG_GET_EXTRA_DATA = "get_extra_data";
    private static final int REQUEST_GET_EXTRA_DATA = 0;

    private Runnable mUpdateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            refreshProjectTime();
            ProjectData projectData = new ProjectData(getApplicationContext());
            if (projectData.isClockedIn(mProjectName)) {
                mTimeUpdater.postDelayed(this, TIME_UPDATE_DELAY);
            }
            projectData.close(getApplicationContext());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        // Get the project name from the Intent
        Intent intent = getIntent();
        mProjectName = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (savedInstanceState != null) {
            mProjectName = savedInstanceState.getString(KEY_PROJECT_NAME);
        }
        // Set the title to the project name
        setTitle(mProjectName);
        // Get a reference to the Clock In/Out button
        mClockInOutButton = (Button)findViewById(R.id.clock_in_out);
        mClockInOutButton.setOnClickListener(this);
        // Get a reference to the Time
        mProjectTime = (TextView)findViewById(R.id.time);
        // Get a reference to the project data
        ProjectData projectData = new ProjectData(this, getString(R.string.default_database_filename));
        ProjectData.Metadata metadata = projectData.getProjectMetadata(mProjectName);
        // Set up the project's settings
        setupToggle(R.id.no_duration, getString(R.string.setting_no_duration_title),
                    getString(R.string.setting_no_duration_description), metadata.noDuration);
        setupToggle(R.id.use_extra_data, getString(R.string.setting_use_extra_data_title),
                getString(R.string.setting_use_extra_data_description), metadata.usesExtraData);
        setupToggle(R.id.track_location, getString(R.string.setting_use_location_title),
                getString(R.string.setting_use_location_description), metadata.trackLocation);
        setupExtraData();
        // Set the text for the clock in/out button.
        if (projectData.isClockedIn(mProjectName)) {
            mClockInOutButton.setText(R.string.clock_out);
        } else {
            mClockInOutButton.setText(metadata.noDuration ?
                    R.string.clock_in_instant : R.string.clock_in);
        }
        projectData.close(this);
        // Set the project time
        refreshProjectTime();
        // Set up the time updater
        mTimeUpdater = new Handler();
        mTimeUpdater.post(mUpdateTimeRunnable);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save the activity state (e.g. project name)
        outState.putString(KEY_PROJECT_NAME, mProjectName);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        // TODO refresh buttons
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        int clickedID = view.getId();
        if (clickedID == R.id.clock_in_out) {
            ProjectData projectData = new ProjectData(this);
            if (projectData.isClockedIn(mProjectName)) {
                projectData.clockOut(mProjectName);
                Snackbar.make(view, R.string.project_clocked_out, Snackbar.LENGTH_SHORT).show();
                // Remove the time updater
                mTimeUpdater.removeCallbacks(mUpdateTimeRunnable);
                // Remove any notification using our broadcast receiver
                sendBroadcast(new ProjectReceiver.IntentBuilder(this, mProjectName)
                        .setAction(ProjectReceiver.ACTION_CLOCK_OUT)
                        .setSuppressToast(true)
                        .build());
                // We're clocked out, so set the text to clock in
                mClockInOutButton.setText(R.string.clock_in);
            } else {
                // Get the project's metadata
                ProjectData.Metadata metadata = projectData.getProjectMetadata(mProjectName);
                // If the project uses extra data, then use the default or if not default, then
                // show a dialog which allows the user to input the extra data
                if (metadata.usesExtraData && TextUtils.isEmpty(metadata.defaultExtraData)) {
                    Bundle extras = new Bundle();
                    extras.putString(KEY_PROJECT_NAME, mProjectName);
                    Dialogs.UserInputDialogFragment.createUserInputDialog(
                                    getString(R.string.dialog_get_extra_data_title), null,
                                    R.string.dialog_get_extra_data_positive_button, null)
                           .setOnResultListener(this)
                           .setRequestCode(REQUEST_GET_EXTRA_DATA)
                           .putExtras(extras)
                           .show(getFragmentManager(),
                                 TAG_DIALOG_GET_EXTRA_DATA);
                } else {
                    // Post a notification using our broadcast receiver
                    ProjectReceiver.IntentBuilder intentBuilder =
                            new ProjectReceiver.IntentBuilder(this, mProjectName)
                                    .setAction(ProjectReceiver.ACTION_CLOCK_IN)
                                    .setSuppressToast(true);
                    if (metadata.usesExtraData) {
                        intentBuilder.setExtraData(metadata.defaultExtraData);
                    }
                    sendBroadcast(intentBuilder.build());
                    // Handle activity specific "clocked in" actions (e.g. showing a Snackbar to
                    // the user and starting the timer. This is done from another function so it
                    // can also be called from the GetExtraDataDialogFragment
                    doPostClockInActions();
                }
            }
            projectData.close(this);
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
                // Allow the user to pick a file destination from the storage
                // access framework
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                // Only allow files which can be opened
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                // Set the MIME type to comma-separated variables
                intent.setType("text/csv");
                // Set a default title
                intent.putExtra(Intent.EXTRA_TITLE, mProjectName + ".csv");
                // Start the activity to get the file
                startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT);
                return true;
            case R.id.mark_notification:
                // The user wants to pin a "sticky" notification to which allows
                // the user to clock in/out or mark items
                sendBroadcast(new ProjectReceiver.IntentBuilder(this, mProjectName)
                        .setAction(ProjectReceiver.ACTION_POST_STICKY)
                        .build());
                return true;
            case R.id.edit:
                // Start the edit activity
                Intent editIntent = new Intent(this, EditActivity.class);
                editIntent.putExtra(Intent.EXTRA_TEXT, mProjectName);
                startActivity(editIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle the Export request from the user
        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                new AsyncTask<Uri, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Uri... uri) {
                        boolean isError;
                        try {
                            OutputStream os = new BufferedOutputStream(
                                    getContentResolver().openOutputStream(uri[0], "w"));
                            ProjectData projectData = new ProjectData(getApplicationContext());
                            isError = !projectData.dumpToCSV(mProjectName, os);
                            projectData.close(getApplicationContext());
                            os.close();
                        } catch (IOException exception) {
                            isError = true;
                        }
                        return isError;
                    }

                    @Override
                    protected void onPostExecute(Boolean isError) {
                        if (isError) {
                            Snackbar.make(mProjectTime,
                                    getString(R.string.exported_error, mProjectName),
                                    Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(mProjectTime,
                                    getString(R.string.exported_success, mProjectName),
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }.execute(uri);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        // Remove the time updater
        mTimeUpdater.removeCallbacks(mUpdateTimeRunnable);
        super.onDestroy();
    }

    private void refreshProjectTime() {
        ProjectData projectData = new ProjectData(this);
        int projectTime = projectData.getProjectTime(mProjectName);
        projectData.close(this);
        String timeText;
        int days = projectTime / SEC_PER_DAY;
        int hours = (projectTime % SEC_PER_DAY) / SEC_PER_HOUR;
        int minutes = (projectTime % SEC_PER_HOUR) / SEC_PER_MIN;
        int seconds = projectTime % SEC_PER_MIN;
        if (projectTime == -1) {
            timeText = getResources().getString(R.string.time_none);
        } else if (days > 0) {
            timeText = getResources().getString(
                    R.string.time_days, days, hours, minutes, seconds);
        } else {
            timeText = getResources().getString(
                    R.string.time_hours, hours, minutes, seconds);
        }
        mProjectTime.setText(timeText);
    }

    private void setupToggle(int toggleRootID, String title, String description, boolean checked) {
        final View rootView = findViewById(toggleRootID);
        ((TextView)rootView.findViewById(R.id.title)).setText(title);
        ((TextView)rootView.findViewById(R.id.description)).setText(description);
        final Switch toggle = rootView.findViewById(R.id.toggle);
        toggle.setChecked(checked);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle.performClick();
            }
        });
        // Enable/disable the extra data
        if (toggleRootID == R.id.use_extra_data) {
            findViewById(R.id.default_extra_label).setEnabled(checked);
            findViewById(R.id.default_extra_data).setEnabled(checked);
        }
        ((Switch)rootView.findViewById(R.id.toggle)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean state) {
                int clickedID = rootView.getId();
                ProjectData projectData = new ProjectData(ProjectActivity.this);
                ProjectData.Metadata metadata = projectData.getProjectMetadata(mProjectName);
                if (clickedID == R.id.no_duration) {
                    metadata.noDuration = state;
                    if (projectData.isClockedIn(mProjectName)) {
                        mClockInOutButton.setText(R.string.clock_out);
                    } else {
                        mClockInOutButton.setText(metadata.noDuration ?
                                R.string.clock_in_instant : R.string.clock_in);
                    }
                } else if (clickedID == R.id.track_location) {
                    metadata.trackLocation = state;
                } else if (clickedID == R.id.use_extra_data) {
                    metadata.usesExtraData = state;
                    // Update the enabled state of the default extra data
                    findViewById(R.id.default_extra_label).setEnabled(state);
                    findViewById(R.id.default_extra_data).setEnabled(state);
                }
                projectData.updateMetadata(mProjectName, metadata);
                projectData.close(getApplicationContext());
            }
        });
    }

    private void setupExtraData() {
        // Set the current default extra data
        EditText input = (EditText)findViewById(R.id.default_extra_data);
        ProjectData projectData = new ProjectData(this);
        ProjectData.Metadata metadata = projectData.getProjectMetadata(mProjectName);
        projectData.close(this);
        input.setText(metadata.defaultExtraData);

        // Add a TextWatcher to update the new default extra data
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) {
                ProjectData projectData = new ProjectData(ProjectActivity.this);
                ProjectData.Metadata metadata = projectData.getProjectMetadata(mProjectName);
                metadata.defaultExtraData = editable == null ? "" : editable.toString();
                projectData.updateMetadata(mProjectName, metadata);
                projectData.close(getApplicationContext());
            }
        });
    }

    private void doPostClockInActions() {
        // Show a message Snackbar message to the user. If the project is not clocked
        // in, then that means the this project is "instant" -- same start/end times
        ProjectData projectData = new ProjectData(this);
        ProjectData.Metadata metadata = projectData.getProjectMetadata(mProjectName);
        projectData.close(this);
        Snackbar.make(mProjectTime, metadata.noDuration ?
                        R.string.project_clocked_in_instant : R.string.project_clocked_in,
                Snackbar.LENGTH_SHORT).show();
        // Add the time updater if not an instant project, and set the button to "clock out"
        if (!metadata.noDuration) {
            mTimeUpdater.post(mUpdateTimeRunnable);
            mClockInOutButton.setText(R.string.clock_out);
        } else {
            mClockInOutButton.setText(R.string.clock_in_instant);
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_GET_EXTRA_DATA && resultCode == Activity.RESULT_OK) {
            // Get the user input
            String extraData = intent.getStringExtra(Dialogs.UserInputDialogFragment.KEY_USER_INPUT);
            // Get the extras set during the dialog fragment
            Bundle extras = intent.getBundleExtra(ResultFragment.EXTRA_BUNDLE);
            String projectName = extras.getString(KEY_PROJECT_NAME);
            // Send the broadcast to clock in with the extra data
            sendBroadcast(new ProjectReceiver.IntentBuilder(this, projectName)
                    .setAction(ProjectReceiver.ACTION_CLOCK_IN)
                    .setExtraData(extraData)
                    .setSuppressToast(true)
                    .build());
            // Handle the post clock/in out actions
            doPostClockInActions();
        }
    }
}
