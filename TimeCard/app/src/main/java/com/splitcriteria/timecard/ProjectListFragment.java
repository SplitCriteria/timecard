package com.splitcriteria.timecard;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Holds a list of the projects. The hosting Activity can show either archived or
 * non-archived projects. The user can swipe left and right to archive, delete, and
 * un-archive projects. This class, when opened, attempts to schedule a backup job
 * if it currently doesn't exist.
 */
public class ProjectListFragment extends ResultFragment implements
        ResultFragment.OnResultListener {

    private static final String TAG_DIALOG_CREATE_PROJECT = "dialog_project";
    private static final String TAG_DIALOG_SIMPLE_MESSAGE = "simple_message";

    private static final int MILLISECONDS_PER_DAY = 86400000;
    private static final int BACKUP_PERIOD = MILLISECONDS_PER_DAY;

    private static final int REQUEST_CODE_CREATE_PROJECT = 1;

    private RecyclerView mRecyclerView;
    private ProjectAdapter mAdapter;
    private GestureDetectorCompat mGestures;
    private boolean mShowingArchived = false;
    private ItemTouchHelper mCurrentProjectsItemTouchHelper;
    private ItemTouchHelper mArchivedProjectsItemTouchHelper;

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

        // Set up the gesture detector to open the ProjectActivity on a user click
        mGestures = new GestureDetectorCompat(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View clicked = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
                if (clicked != null) {
                    TextView tv = clicked.findViewById(R.id.name);
                    String projectName = tv.getText().toString();
                    openProjectActivity(projectName);
                    return true;
                } else {
                    return false;
                }
            }
        });

        // Add a OnItemTouchListener to collect information for the GestureDetector
        mRecyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                mGestures.onTouchEvent(e);
                return super.onInterceptTouchEvent(rv, e);
            }
        });

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

    private void openProjectActivity(String projectName) {
        if (!TextUtils.isEmpty(projectName)) {
            Intent projectIntent = new Intent(getActivity(), ProjectActivity.class);
            projectIntent.putExtra(Intent.EXTRA_TEXT, projectName);
            startActivity(projectIntent);
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
                        // Open the project activity for the newly created project
                        openProjectActivity(projectName);
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
