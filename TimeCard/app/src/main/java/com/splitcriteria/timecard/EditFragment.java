package com.splitcriteria.timecard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;

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
 * A Fragment which allows a user to edit data contained in the ProjectData project
 */

public class EditFragment extends ResultFragment implements
        ResultFragment.OnResultListener,
        RowAdapter.OnRowClickListener {

    private static final String TAG_DIALOG_PICK_DATE = "dialog_pick_date";
    private static final String TAG_DIALOG_PICK_TIME = "dialog_pick_time";
    private static final String TAG_DIALOG_ALERT = "dialog_alert";
    private static final String TAG_DIALOG_UPDATE_EXTRA_DATA = "dialog_update_extra_data";

    private static final int REQUEST_CODE_PICK_START_DATE = 0;
    private static final int REQUEST_CODE_PICK_START_TIME = 1;
    private static final int REQUEST_CODE_PICK_END_DATE = 2;
    private static final int REQUEST_CODE_PICK_END_TIME = 3;
    private static final int REQUEST_CODE_ENTER_EXTRA_DATA = 4;

    private static final String KEY_PROJECT_NAME = "project_name";
    private static final String KEY_POSITION = "position";

    private RecyclerView mRecyclerView;
    private RowAdapter mAdapter;
    private String mProjectName;
    private ItemTouchHelper mRowItemTouchHelper;

    public EditFragment() { }

    public static EditFragment createEditFragment(String projectName) {
        EditFragment editFragment = new EditFragment();
        Bundle args = new Bundle();
        args.putString(KEY_PROJECT_NAME, projectName);
        editFragment.setArguments(args);
        return editFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_edit, container, false);

        // Get the project name from the intent
        mProjectName = getArguments().getString(KEY_PROJECT_NAME);

        // Populate the Recycler View with test items
        mRecyclerView = root.findViewById(R.id.rows);
        // Improves performance for fixed-size RecyclerView's
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);

        // Create an ItemTouchHelper to handle swipe events for current projects (i.e. allow
        // user to swipe right to archive a project)
        mRowItemTouchHelper = new ItemTouchHelper(
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
                            // Get the row information
                            final int adapterPosition = viewHolder.getAdapterPosition();
                            // Remove the row from the adapter
                            final ProjectData.Row row = mAdapter.remove(adapterPosition);
                            // Add a snackbar message with the ability to UNDO the action
                            final Snackbar sb = Snackbar.make(mRecyclerView,
                                    getString(R.string.notification_row_deleted),
                                    Snackbar.LENGTH_LONG);
                            // Set an undo action which doesn't actually do anything since
                            // the deletion doesn't occur until the Snackbar has timed out
                            sb.setAction(R.string.undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Unremove the row if the user clicked Undow
                                    mAdapter.unRemove(row.id);
                                    mRecyclerView.scrollToPosition(adapterPosition);
                                }
                            });
                            // Add a callback which removes the row from the adapter and
                            // deletes it in the database
                            sb.addCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar transientBottomBar, int event) {
                                    if (event != DISMISS_EVENT_ACTION) {
                                        // Remove the row from the database
                                        Context context = sb.getContext();
                                        ProjectData projectData = new ProjectData(context);
                                        projectData.deleteRow(mProjectName, row.id);
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

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshRows();
    }

    private void refreshRows() {
        ProjectData projectData = new ProjectData(getActivity());
        mAdapter = new RowAdapter(projectData.getRows(mProjectName));
        projectData.close(getActivity());
        mAdapter.addOnRowClickListener(this);
        mRecyclerView.swapAdapter(mAdapter, true);
        mRowItemTouchHelper.attachToRecyclerView(mRecyclerView);
        getActivity().setTitle(getString(R.string.title_edit_project, mProjectName));
    }

    @Override
    public void onRowDataClick(int position, ProjectData.Row row, int dataType) {
        // Create extra data which will be attached to the dialog
        Bundle extras = new Bundle();
        extras.putInt(KEY_POSITION, position);
        // Show the user an appropriate picker dialog
        switch (dataType) {
            case RowAdapter.START_DATE:
                Dialogs.PickDateDialogFragment.createPickDateDialogFragment(row.startTime)
                        .setRequestCode(REQUEST_CODE_PICK_START_DATE)
                        .setOnResultListener(this)
                        .putExtras(extras)
                        .show(getFragmentManager(), TAG_DIALOG_PICK_DATE);
                break;
            case RowAdapter.START_TIME:
                Dialogs.PickTimeDialogFragment.createPickTimeDialogFragment(row.startTime)
                        .setRequestCode(REQUEST_CODE_PICK_START_TIME)
                        .setOnResultListener(this)
                        .putExtras(extras)
                        .show(getFragmentManager(), TAG_DIALOG_PICK_TIME);
                break;
            case RowAdapter.END_DATE:
                // An empty end date/time means the project is still clocked in
                // -- don't allow the user to change a non-clocked in value
                if (row.endTime == null) {
                    Dialogs.SimpleMessageDialogFragment.createSimpleMessageDialog(
                            getString(R.string.error_title),
                            getString(R.string.dialog_immutable_not_clocked_in))
                            .show(getFragmentManager(), TAG_DIALOG_ALERT);
                } else {
                    Dialogs.PickDateDialogFragment.createPickDateDialogFragment(row.endTime)
                            .setRequestCode(REQUEST_CODE_PICK_END_DATE)
                            .setOnResultListener(this)
                            .putExtras(extras)
                            .show(getFragmentManager(), TAG_DIALOG_PICK_DATE);
                }
                break;
            case RowAdapter.END_TIME:
                // An empty end date/time means the project is still clocked in
                // -- don't allow the user to change a non-clocked in value
                if (row.endTime == null) {
                    Dialogs.SimpleMessageDialogFragment.createSimpleMessageDialog(
                            getString(R.string.error_title),
                            getString(R.string.dialog_immutable_not_clocked_in))
                            .show(getFragmentManager(), TAG_DIALOG_ALERT);
                } else {
                    Dialogs.PickTimeDialogFragment.createPickTimeDialogFragment(row.endTime)
                            .setRequestCode(REQUEST_CODE_PICK_END_TIME)
                            .setOnResultListener(this)
                            .putExtras(extras)
                            .show(getFragmentManager(), TAG_DIALOG_PICK_TIME);
                }
                break;
            case RowAdapter.EXTRA_DATA:
                Dialogs.UserInputDialogFragment.createUserInputDialog(
                        getString(R.string.dialog_get_extra_data_title), null,
                        R.string.dialog_update_extra_data_positive_button, row.extraData)
                        .setRequestCode(REQUEST_CODE_ENTER_EXTRA_DATA)
                        .setOnResultListener(this)
                        .putExtras(extras)
                        .show(getFragmentManager(), TAG_DIALOG_UPDATE_EXTRA_DATA);
                break;
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            Bundle extras = intent.getBundleExtra(ResultFragment.EXTRA_BUNDLE);
            int position = extras.getInt(KEY_POSITION);
            ProjectData.Row row = mAdapter.getRow(position);
            switch (requestCode) {
                case REQUEST_CODE_PICK_START_DATE:
                case REQUEST_CODE_PICK_END_DATE:
                    int year = intent.getIntExtra(
                            Dialogs.PickDateDialogFragment.KEY_YEAR, 0);
                    int month = intent.getIntExtra(
                            Dialogs.PickDateDialogFragment.KEY_MONTH, 0);
                    int day = intent.getIntExtra(
                            Dialogs.PickDateDialogFragment.KEY_DAY_OF_MONTH, 0);
                    if (requestCode == REQUEST_CODE_PICK_START_DATE) {
                        row.startTime.set(year, month, day);
                    } else {
                        row.endTime.set(year, month, day);
                    }
                    break;
                case REQUEST_CODE_PICK_START_TIME:
                case REQUEST_CODE_PICK_END_TIME:
                    int hourOfDay = intent.getIntExtra(
                            Dialogs.PickTimeDialogFragment.KEY_HOUR_OF_DAY, 0);
                    int minute = intent.getIntExtra(
                            Dialogs.PickTimeDialogFragment.KEY_MINUTE, 0);
                    Calendar time = requestCode == REQUEST_CODE_PICK_START_TIME ?
                            row.startTime : row.endTime;
                    time.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    time.set(Calendar.MINUTE, minute);
                    break;
                case REQUEST_CODE_ENTER_EXTRA_DATA:
                    row.extraData = intent.getStringExtra(
                            Dialogs.UserInputDialogFragment.KEY_USER_INPUT);
                    break;
            }
            // Update the row data
            ProjectData projectData = new ProjectData(getActivity());
            projectData.updateRow(mProjectName, row);
            projectData.close(getActivity());
            // Notify the adapter of the update so it can refresh itself
            mAdapter.notifyItemChanged(position);
        }
    }

}
