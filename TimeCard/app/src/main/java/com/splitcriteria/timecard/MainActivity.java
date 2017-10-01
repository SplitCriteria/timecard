package com.splitcriteria.timecard;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

/**
 * Main entry point for the app. Contains the ProjectListFragment at a minimum
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG_PROJECT_LIST_FRAGMENT =
            "com.splitcriteria.timecard.TAG_PROJECT_LIST_FRAGMENT";

    private ProjectListFragment mProjectListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set the default preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Release any unresolved database locks
        DatabaseLock.forceRelease(this);

        // Add the project list fragment if it doesn't exist
        mProjectListFragment = (ProjectListFragment)getFragmentManager()
                .findFragmentByTag(TAG_PROJECT_LIST_FRAGMENT);
        if (mProjectListFragment == null) {
            mProjectListFragment = new ProjectListFragment();
            getFragmentManager().beginTransaction()
                                .add(R.id.container, mProjectListFragment,
                                     TAG_PROJECT_LIST_FRAGMENT)
                                .commit();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProjectListFragment.createNewProject();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Refresh the toolbar title
        refreshToolbarTitle();
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

    private void refreshToolbarTitle() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(mProjectListFragment.isShowingArchived() ?
                R.string.title_archived_projects : R.string.title_current_projects);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.current_projects && mProjectListFragment.isShowingArchived()) {
            // User wants to see current projects and they're not already shown
            mProjectListFragment.showArchivedProjects(false);
            refreshToolbarTitle();
        } else if (id == R.id.archived_projects && !mProjectListFragment.isShowingArchived()) {
            // User wants to see the archived projects and they're not already shown
            mProjectListFragment.showArchivedProjects(true);
            refreshToolbarTitle();
        } else if (id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
