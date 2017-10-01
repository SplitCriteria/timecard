package com.splitcriteria.timecard;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
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
 * Activity which contains an EditFragment
 */
public class EditActivity extends AppCompatActivity {

    private static final String TAG_EDIT_FRAGMENT = "com.splitcriteria.timecard.EDIT_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container);

        FragmentManager fragmentManager = getFragmentManager();

        EditFragment editFragment = (EditFragment)fragmentManager
                .findFragmentByTag(TAG_EDIT_FRAGMENT);
        if (editFragment == null) {
            String projectName = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            editFragment = EditFragment.createEditFragment(projectName);
            fragmentManager.beginTransaction()
                           .add(R.id.container, editFragment, TAG_EDIT_FRAGMENT)
                           .commit();
        }
    }

}
