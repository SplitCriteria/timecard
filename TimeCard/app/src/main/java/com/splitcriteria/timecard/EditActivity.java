package com.splitcriteria.timecard;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import java.util.Calendar;

public class EditActivity extends AppCompatActivity {

    private static final String TAG_EDIT_FRAGMENT = "com.splitcriteria.timecard.EDIT_FRAGMENT";

    private EditFragment mEditFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container);

        FragmentManager fragmentManager = getFragmentManager();

        mEditFragment = (EditFragment)fragmentManager.findFragmentByTag(TAG_EDIT_FRAGMENT);
        if (mEditFragment == null) {
            String projectName = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            mEditFragment = EditFragment.createEditFragment(projectName);
            fragmentManager.beginTransaction()
                           .add(R.id.container, mEditFragment, TAG_EDIT_FRAGMENT)
                           .commit();
        }
    }

}
