package com.splitcriteria.timecard;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
 * Holds a list of the projects. The hosting Activity can show either archived or
 * non-archived projects. The user can swipe left and right to archive, delete, and
 * un-archive projects. This class, when opened, attempts to schedule a backup job
 * if it currently doesn't exist.
 */
public class ProjectListFragment extends ResultFragment implements
        ResultFragment.OnResultListener,
        ProjectAdapter.OnProjectClickedListener,
        PopupMenu.OnDismissListener,
        PopupMenu.OnMenuItemClickListener {

    private static final String TAG_DIALOG_CREATE_PROJECT = "dialog_project";
    private static final String TAG_DIALOG_SIMPLE_MESSAGE = "simple_message";
    private static final String TAG_DIALOG_GET_EXTRA_DATA = "get_extra_data";

    private static final int MILLISECONDS_PER_DAY = 86400000;
    private static final int BACKUP_PERIOD = MILLISECONDS_PER_DAY;

    private static final int REQUEST_GET_EXTRA_DATA = 0;
    private static final int REQUEST_CODE_CREATE_PROJECT = 1;
    private static final int REQUEST_CODE_CREATE_DOCUMENT = 2;

    private static final String KEY_PROJECT_NAME = "project_name";

    private RecyclerView mRecyclerView;
    private ProjectAdapter mAdapter;
    private GestureDetectorCompat mGestures;
    private boolean mShowingArchived = false;
    private ItemTouchHelper mCurrentProjectsItemTouchHelper;
    private ItemTouchHelper mArchivedProjectsItemTouchHelper;
    private String mSelectedProjectName;

    private static final int JOB_ID_BACKUP_SERVICE = 1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.content_main, container, false);

        // Populate the Recycler View with test items
        mRecyclerView = root.findViewById(R.id.projects);
        // Improves performance for fixed-size RecyclerView's
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);

//        // Set up the gesture detector to open the ProjectActivity on a user click
//        mGestures = new GestureDetectorCompat(getActivity(), new GestureDetector.SimpleOnGestureListener() {
//            @Override
//            public boolean onSingleTapUp(MotionEvent e) {
//                View clicked = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
//                if (clicked != null) {
//                    TextView tv = clicked.findViewById(R.id.name);
//                    String projectName = tv.getText().toString();
//                    openProjectActivity(projectName);
//                    return true;
//                } else {
//                    return false;
//                }
//            }
//        });

//        // Add a OnItemTouchListener to collect information for the GestureDetector
//        mRecyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
//            @Override
//            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
//                mGestures.onTouchEvent(e);
//                return super.onInterceptTouchEvent(rv, e);
//            }
//        });

        // Create an ItemTouchHelper to handle swipe events for current projects (i.e. allow
        // user to swipe right to archive a project)
        mCurrentProjectsItemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                // No up/down moves implemented
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // Archive on a right swipe
                if (direction == ItemTouchHelper.RIGHT) {
                    // Get the project name
                    final int adapterPosition = viewHolder.getAdapterPosition();
                    final String projectName = mAdapter.getProjectName(adapterPosition);
                    // Remove the project from the adapter
                    mAdapter.remove(viewHolder.getAdapterPosition());
                    // Add a snackbar message with the ability to UNDO the action
                    final Snackbar sb = Snackbar.make(mRecyclerView,
                            getString(R.string.project_archived, projectName),
                            Snackbar.LENGTH_LONG);
                    // Set an undo action
                    sb.setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // The project is only archived after the dismissal, so we
                            // just need to add the project name back to the adapter
                            mAdapter.add(projectName);
                        }
                    });
                    sb.addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            if (event != DISMISS_EVENT_ACTION) {
                                // The snackbar timed out, or was dismissed
                                // -- archive the project
                                Context context = sb.getContext();
                                ProjectData projectData = new ProjectData(context);
                                projectData.setArchived(projectName, true);
                                projectData.close(context);
                            }
                            super.onDismissed(transientBottomBar, event);
                        }
                    });
                    // Show the snackbar
                    sb.show();
                }
            }
        });

        // Create an ItemTouchHelper to handle swipe events for archived projects (i.e. allow
        // user to swipe right to delete a project and swipe left to unarchive a project)
        mArchivedProjectsItemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        // No up/down moves implemented
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        // Get the project name
                        final int adapterPosition = viewHolder.getAdapterPosition();
                        final String projectName = mAdapter.getProjectName(adapterPosition);
                        // Remove the project from the adapter
                        mAdapter.remove(viewHolder.getAdapterPosition());
                        final boolean delete = (direction == ItemTouchHelper.RIGHT);
                        // Add a snackbar message with the ability to UNDO the action
                        final Snackbar sb = Snackbar.make(mRecyclerView,
                                getString(delete ? R.string.project_deleted :
                                                   R.string.project_unarchived,
                                          projectName),
                                Snackbar.LENGTH_LONG);
                        // Set an undo action
                        sb.setAction(R.string.undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // The project is only deleted/un-archived after the dismissal,
                                // so we just need to add the project name back to the adapter
                                mAdapter.add(projectName);
                            }
                        });
                        // Add a callback to handle the deletion or un-archive after the
                        // snackbar is dismissed (or timed out)
                        sb.addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    // The snackbar timed out, or was dismissed
                                    // -- delete/un-archive the project
                                    Context context = sb.getContext();
                                    ProjectData projectData = new ProjectData(context);
                                    if (delete) {
                                        projectData.deleteProject(projectName);
                                    } else {
                                        projectData.setArchived(projectName, false);
                                    }
                                    projectData.close(context);
                                }
                                super.onDismissed(transientBottomBar, event);
                            }
                        });
                        // Show the snackbar
                        sb.show();
                    }
                });

        // Refresh the project names
        refreshProjectNames();
        // TODO test
        scheduleBackupJob();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.addOnProjectClickedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.removeOnProjectClickedListener(this);
    }

    private void refreshProjectNames() {
        ProjectData projectData = new ProjectData(getActivity());
        mAdapter = new ProjectAdapter(projectData.getProjectNames(mShowingArchived));
        projectData.close(getActivity());
        mRecyclerView.swapAdapter(mAdapter, true);
        if (mShowingArchived) {
            mCurrentProjectsItemTouchHelper.attachToRecyclerView(null);
            mArchivedProjectsItemTouchHelper.attachToRecyclerView(mRecyclerView);
        } else {
            mArchivedProjectsItemTouchHelper.attachToRecyclerView(null);
            mCurrentProjectsItemTouchHelper.attachToRecyclerView(mRecyclerView);
        }
    }

    public void showArchivedProjects(boolean showArchived) {
        mShowingArchived = showArchived;
        refreshProjectNames();
    }

    public boolean isShowingArchived() {
        return mShowingArchived;
    }

    private void alert(String title, String message) {
        Dialogs.SimpleMessageDialogFragment.createSimpleMessageDialog(title, message)
                .show(getFragmentManager(), TAG_DIALOG_SIMPLE_MESSAGE);
    }

    public void createNewProject() {
        Dialogs.UserInputDialogFragment.createUserInputDialog(
                getString(R.string.title_create_project), null, R.string.button_create, null)
              .setRequestCode(REQUEST_CODE_CREATE_PROJECT)
              .setOnResultListener(this)
              .show(getFragmentManager(), TAG_DIALOG_CREATE_PROJECT);
    }

    @Override
    public void onClockInOutClicked(String projectName) {
        ProjectData projectData = new ProjectData(getActivity());
        ProjectData.Metadata metadata = projectData.getProjectMetadata(projectName);
        // Check to see if there is supposed to be extra data
        if (metadata.usesExtraData && TextUtils.isEmpty(metadata.defaultExtraData)) {
            // Get the extra data from the user
            Bundle extras = new Bundle();
            // Add the project name to the dialog to receive it in the onResult method
            extras.putString(KEY_PROJECT_NAME, projectName);
            Dialogs.UserInputDialogFragment.createUserInputDialog(
                    getString(R.string.dialog_get_extra_data_title), null,
                    R.string.dialog_get_extra_data_positive_button, null)
                    .setOnResultListener(this)
                    .setRequestCode(REQUEST_GET_EXTRA_DATA)
                    .putExtras(extras)
                    .show(getFragmentManager(), TAG_DIALOG_GET_EXTRA_DATA);
        } else {
            // Clock in/out the project
            boolean isClockedIn = projectData.isClockedIn(projectName);
            ProjectReceiver.IntentBuilder intentBuilder = new ProjectReceiver
                    .IntentBuilder(getActivity(), projectName)
                    .setAction(isClockedIn ? ProjectReceiver.ACTION_CLOCK_OUT :
                                             ProjectReceiver.ACTION_CLOCK_IN)
                    .setSuppressToast(true);
            // If this project uses extra data, then add it now
            if (metadata.usesExtraData) {
                intentBuilder.setExtraData(metadata.defaultExtraData);
            }
            // Send a broadcast which actually clocks in/out the project
            getActivity().sendBroadcast(intentBuilder.build());
            // Use a Snackbar to notify the user
            Snackbar.make(mRecyclerView, getString(
                    metadata.noDuration ? R.string.project_clocked_in_instant :
                                          isClockedIn ? R.string.project_clocked_out :
                                                        R.string.project_clocked_in, projectName),
                    Snackbar.LENGTH_SHORT).show();
            // TODO change the button symbology
        }
        projectData.close(getActivity());
    }

    @Override
    public void onSettingsClicked(View view, String projectName) {
        mSelectedProjectName = projectName;
        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.project_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.setOnDismissListener(this);
        popup.show();
    }

    @Override
    public void onDismiss(PopupMenu popupMenu) {
        mSelectedProjectName = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.settings) {
            Intent projectIntent = new Intent(getActivity(), ProjectSettingsActivity.class);
            projectIntent.putExtra(Intent.EXTRA_TEXT, mSelectedProjectName);
            startActivity(projectIntent);
        } else if (id == R.id.edit) {
            Intent projectIntent = new Intent(getActivity(), EditActivity.class);
            projectIntent.putExtra(Intent.EXTRA_TEXT, mSelectedProjectName);
            startActivity(projectIntent);
        } else if (id == R.id.export) {
            // Allow the user to pick a file destination from the storage
            // access framework
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            // Only allow files which can be opened
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // Set the MIME type to comma-separated variables
            intent.setType("text/csv");
            // Set a default title
            intent.putExtra(Intent.EXTRA_TITLE, mSelectedProjectName + ".csv");
            // Start the activity to get the file
            startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT);
        } else if (id == R.id.mark_notification) {
            // The user wants to pin a "sticky" notification to which allows
            // the user to clock in/out or mark items
            getActivity().sendBroadcast(new ProjectReceiver.IntentBuilder(
                        getActivity(), mSelectedProjectName)
                    .setAction(ProjectReceiver.ACTION_POST_STICKY)
                    .build());
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                new AsyncTask<Uri, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Uri... uri) {
                        boolean isError;
                        try {
                            OutputStream os = getActivity().getContentResolver()
                                    .openOutputStream(uri[0], "w");
                            if (os == null) {
                                isError = true;
                            } else {
                                os = new BufferedOutputStream(os);
                                ProjectData projectData = new ProjectData(getActivity());
                                isError = !projectData.dumpToCSV(mSelectedProjectName, os);
                                projectData.close(getActivity());
                                os.close();
                            }
                        } catch (IOException exception) {
                            isError = true;
                        }
                        return isError;
                    }

                    @Override
                    protected void onPostExecute(Boolean isError) {
                        if (isError) {
                            Snackbar.make(mRecyclerView,
                                    getString(R.string.exported_error, mSelectedProjectName),
                                    Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(mRecyclerView,
                                    getString(R.string.exported_success, mSelectedProjectName),
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
    public void onResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_CREATE_PROJECT && resultCode == Activity.RESULT_OK) {
            String projectName = intent.getExtras()
                    .getString(Dialogs.UserInputDialogFragment.KEY_USER_INPUT);
            if (!TextUtils.isEmpty(projectName)) {
                Activity activity = getActivity();
                ProjectData projectData = new ProjectData(
                        getActivity(), getString(R.string.default_database_filename));
                // If the project already exists, then fail
                if (projectData.exists(projectName)) {
                    alert(getString(R.string.error_title),
                            getString(R.string.error_project_exists, projectName));
                } else {
                    // Add the project if it doesn't already exist
                    if (projectData.addProject(projectName)) {
                        Snackbar.make(
                                mRecyclerView, R.string.project_created,
                                Snackbar.LENGTH_SHORT).show();
                        // Open the project settings for the newly created project
                        Intent settingsIntent = new Intent(
                                getActivity(), ProjectSettingsActivity.class);
                        settingsIntent.putExtra(Intent.EXTRA_TEXT, projectName);
                        startActivity(settingsIntent);
                    } else {
                        // Notify the user if that the project name already exists
                        DialogFragment alert = Dialogs.SimpleMessageDialogFragment
                                .createSimpleMessageDialog(
                                        getString(R.string.error_title),
                                        getString(R.string.error_project_exists, projectName));
                        alert.show(getFragmentManager(), TAG_DIALOG_SIMPLE_MESSAGE);

                        alert(activity.getString(R.string.error_title),
                                activity.getString(R.string.error_create, projectName));
                    }
                    refreshProjectNames();
                }
                projectData.close(activity);
            }
        } else if (requestCode == REQUEST_GET_EXTRA_DATA && resultCode == Activity.RESULT_OK) {
            // Get the project name from the extras
            Bundle extras = intent.getBundleExtra(ResultFragment.EXTRA_BUNDLE);
            String projectName = extras.getString(KEY_PROJECT_NAME);
            // Clock in/out the project
            ProjectData projectData = new ProjectData(getActivity());
            ProjectData.Metadata metadata = projectData.getProjectMetadata(projectName);
            boolean isClockedIn = projectData.isClockedIn(projectName);
            projectData.close(getActivity());
            ProjectReceiver.IntentBuilder intentBuilder = new ProjectReceiver
                    .IntentBuilder(getActivity(), projectName)
                    .setAction(isClockedIn ? ProjectReceiver.ACTION_CLOCK_OUT :
                                             ProjectReceiver.ACTION_CLOCK_IN)
                    .setExtraData(intent.getStringExtra(
                            Dialogs.UserInputDialogFragment.KEY_USER_INPUT))
                    .setSuppressToast(true);
            // Send a broadcast which actually clocks in/out the project
            getActivity().sendBroadcast(intentBuilder.build());
            // Use a Snackbar to notify the user
            Snackbar.make(mRecyclerView, getString(
                    metadata.noDuration ? R.string.project_clocked_in_instant :
                                          isClockedIn ? R.string.project_clocked_out :
                                                        R.string.project_clocked_in, projectName),
                    Snackbar.LENGTH_SHORT).show();
            // TODO change the button symbology
            // TODO combine this code with the one above to reduce
        }
    }

    @TargetApi(24)
    private boolean scheduleBackupJob() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobScheduler jobScheduler = (JobScheduler) getActivity().getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
            // See if the job information already exists
            JobInfo jobInfo = jobScheduler.getPendingJob(JOB_ID_BACKUP_SERVICE);
            if (jobInfo == null) {
                // If it doesn't, then create it now
                JobInfo.Builder jobBuilder = new JobInfo.Builder(
                        JOB_ID_BACKUP_SERVICE,
                        new ComponentName(getActivity().getPackageName(),
                                          BackupService.class.getName()));
                jobInfo = jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                                    .setPeriodic(BACKUP_PERIOD)
                                    .setPersisted(true)
                                    .setRequiresCharging(true)
                                    .build();
                int result = jobScheduler.schedule(jobInfo);
                return result == JobScheduler.RESULT_SUCCESS;
            } else {
                // The job already exists
                return true;
            }
        } else {
            // SDK doesn't allow using JobScheduler
            // TODO Use another backup method
            return false;
        }
    }
}
