package com.splitcriteria.timecard;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String PROJECTS_DB_NAME = "projects.db";
    private static final String TAG_CREATE_PROJECT_DIALOG = "dialog_project";

    private RecyclerView mRecyclerView;
    private ProjectAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ProjectData mProjectData;
    private GestureDetectorCompat mGestures;
    private boolean mShowingArchived = false;
    private ItemTouchHelper mCurrentProjectsItemTouchHelper;
    private ItemTouchHelper mArchivedProjectsItemTouchHelper;

    /**
     * Dialog Fragment designed to collect the initial project information from the user
     */
    public static class CreateProjectDialogFragment extends DialogFragment {

        public CreateProjectDialogFragment() {}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View view = LayoutInflater.from(getActivity())
                    .inflate(R.layout.dialog_create_project, null);
            final EditText editText = view.findViewById(R.id.name);
            builder.setView(view)
                   .setTitle(R.string.title_create_project)
                   .setPositiveButton(R.string.button_create,
                           new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                           // Add the new project name
                           String name = editText.getText().toString();
                           boolean created = false;
                           if (!TextUtils.isEmpty(name)) {
                               // TODO Check against current projects
                               created = ((MainActivity) getActivity())
                                       .mProjectData.addProject(name);
                               ((MainActivity) getActivity()).refreshProjectNames();
                           }
                           if (created) {
                               Snackbar.make(((MainActivity)getActivity()).mRecyclerView,
                                       R.string.project_created, Snackbar.LENGTH_SHORT)
                                       .show();
                           } else {
                               // TODO Provide error dialog to user instead of toast
                               Toast.makeText(getActivity(), "Couldn't create '" + name + "'",
                                       Toast.LENGTH_SHORT).show();
                           }
                       }
                   })
                   .setNegativeButton(android.R.string.cancel, null);
            return builder.create();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CreateProjectDialogFragment().show(getFragmentManager(),
                        TAG_CREATE_PROJECT_DIALOG);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Populate the Recycler View with test items
        mRecyclerView = (RecyclerView) findViewById(R.id.projects);
        // Improves performance for fixed-size RecyclerView's
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        // Get the project data
        mProjectData = new ProjectData(getApplicationContext(), PROJECTS_DB_NAME);

        // Set up the gesture detector to open the ProjectActivity on a user click
        mGestures = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View clicked = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
                if (clicked != null) {
                    TextView tv = clicked.findViewById(R.id.name);
                    String projectName = tv.getText().toString();
                    Intent projectIntent = new Intent(getApplicationContext(),
                                                      ProjectActivity.class);
                    projectIntent.putExtra(Intent.EXTRA_TEXT, projectName);
                    startActivity(projectIntent);
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
                    Snackbar sb = Snackbar.make(mRecyclerView,
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
                                mProjectData.setArchived(projectName, true);
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
                        Snackbar sb = Snackbar.make(mRecyclerView,
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
                                    if (delete) {
                                        mProjectData.deleteProject(projectName);
                                    } else {
                                        mProjectData.setArchived(projectName, false);
                                    }
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
    }

    private void refreshProjectNames() {
        mAdapter = new ProjectAdapter(mProjectData.getProjectNames(mShowingArchived));
        mRecyclerView.swapAdapter(mAdapter, true);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mShowingArchived) {
            mCurrentProjectsItemTouchHelper.attachToRecyclerView(null);
            mArchivedProjectsItemTouchHelper.attachToRecyclerView(mRecyclerView);
            toolbar.setTitle(R.string.title_archived_projects);
        } else {
            mArchivedProjectsItemTouchHelper.attachToRecyclerView(null);
            mCurrentProjectsItemTouchHelper.attachToRecyclerView(mRecyclerView);
            toolbar.setTitle(R.string.title_current_projects);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        mProjectData.close();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.current_projects && mShowingArchived) {
            // User wants to see current projects and they're not already shown
            mShowingArchived = false;
            refreshProjectNames();
        } else if (id == R.id.archived_projects && !mShowingArchived) {
            // User wants to see the archived projects and they're not already shown
            mShowingArchived = true;
            refreshProjectNames();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
