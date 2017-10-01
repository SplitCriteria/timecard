package com.splitcriteria.timecard;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
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
