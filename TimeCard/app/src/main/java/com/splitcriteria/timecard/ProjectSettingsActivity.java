package com.splitcriteria.timecard;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

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
 * Activity which holds a Fragment allowing the user to change project
 * specific preferences
 */

public class ProjectSettingsActivity extends AppCompatActivity {

    private static final String TAG_PROJECT_SETTINGS_FRAGMENT =
            "com.splitcriteria.timecard.PROJECT_SETTINGS_FRAGMENT";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the project name from the Intent
        String projectName = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        // Set the action bar title
        setTitle(projectName);
        // Get any existing fragment
        ProjectSettingsFragment fragment = (ProjectSettingsFragment)getFragmentManager()
                .findFragmentByTag(TAG_PROJECT_SETTINGS_FRAGMENT);
        if (fragment == null) {
            // Create the fragment
            fragment = ProjectSettingsFragment.createProjectSettingsFragment(projectName);
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit();
        }
    }
}