package com.splitcriteria.timecard;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Deuce on 9/9/17.
 */

class RowAdapter extends RecyclerView.Adapter<RowAdapter.ViewHolder> implements
        View.OnClickListener {

    private List<ProjectData.Row> mRows;
    private List<RemovedRow> mRemoved;
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private List<OnRowClickListener> mOnClickListeners;

    private class RemovedRow {
        int position;
        ProjectData.Row row;
    }

    static final int START_DATE = 0;
    static final int START_TIME = 1;
    static final int END_DATE = 2;
    static final int END_TIME = 3;
    static final int EXTRA_DATA = 4;

    /**
     * Interface defining callbacks for user interaction with the row adapter
     */
    interface OnRowClickListener {
        /**
         * Callback which is called when the user has clicked on a date, time,
         * or extra data in the RowAdapter.
         *
         * @param position  the position, in the Adapter, of the row clicked
         * @param row       the Row data
         * @param dataType  either START_DATE, START_TIME, END_DATE, END_TIME, or EXTRA_DATA
         */
        void onRowDataClick(int position, ProjectData.Row row, int dataType);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mStartDate;
        TextView mStartTime;
        TextView mEndDate;
        TextView mEndTime;
        TextView mExtra;
        TextView mExtraHeader;
        ViewHolder(CardView v) {
            super(v);
            mStartDate = v.findViewById(R.id.start_date);
            mStartTime = v.findViewById(R.id.start_time);
            mEndDate = v.findViewById(R.id.end_date);
            mEndTime = v.findViewById(R.id.end_time);
            mExtra = v.findViewById(R.id.extra);
            mExtraHeader = v.findViewById(R.id.header_extra);
        }
    }

    RowAdapter(List<ProjectData.Row> rows) {
        // Add all the project names
        mRows = rows;
        mRemoved = new ArrayList<>();
        mOnClickListeners = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView root = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row, parent, false);
        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ProjectData.Row row = mRows.get(position);
        Date startTime = row.startTime.getTime();
        Date endTime = row.endTime == null ? null : row.endTime.getTime();
        holder.mStartDate.setText(mDateFormat.format(startTime));
        holder.mStartDate.setTag(R.id.edit_cell_position, position);
        holder.mStartDate.setOnClickListener(this);
        holder.mStartTime.setText(mTimeFormat.format(startTime));
        holder.mStartTime.setTag(R.id.edit_cell_position, position);
        holder.mStartTime.setOnClickListener(this);
        // It's possible for the end datetime to be NULL if clocked in
        holder.mEndDate.setText(endTime != null ? mDateFormat.format(endTime) : null);
        holder.mEndDate.setTag(R.id.edit_cell_position, position);
        holder.mEndDate.setOnClickListener(this);
        holder.mEndTime.setText(endTime != null ? mTimeFormat.format(endTime) : null);
        holder.mEndTime.setTag(R.id.edit_cell_position, position);
        holder.mEndTime.setOnClickListener(this);
        holder.mExtra.setText(row.extraData);
        holder.mExtra.setTag(R.id.edit_cell_position, position);
        holder.mExtra.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    ProjectData.Row getRow(int position) {
        return mRows.get(position);
    }

    ProjectData.Row remove(int adapterPosition) {
        ProjectData.Row row = mRows.remove(adapterPosition);
        if (row != null) {
            // Update the adapter
            notifyItemRemoved(adapterPosition);
            // Cache the row in the unremoved list
            RemovedRow removedRow = new RemovedRow();
            removedRow.position = adapterPosition;
            removedRow.row = row;
            mRemoved.add(removedRow);
        }
        return row;
    }

    boolean unRemove(int rowId) {
        RemovedRow unremoved = null;
        for (RemovedRow removed : mRemoved) {
            if (removed.row.id == rowId) {
                mRows.add(removed.position, removed.row);
                notifyItemInserted(removed.position);
                // Save a pointer to the object to remove it later
                unremoved = removed;
                break;
            }
        }
        // If a row was reinserted, then remove it from the removed list
        if (unremoved !=  null) {
            mRemoved.remove(unremoved);
            return true;
        } else {
            return false;
        }
    }

    void addOnRowClickListener(OnRowClickListener listener) {
        if (!mOnClickListeners.contains(listener)) {
            mOnClickListeners.add(listener);
        }
    }

    void removeOnRowClickListener(OnRowClickListener listener) {
        mOnClickListeners.remove(listener);
    }

    @Override
    public void onClick(View view) {
        if (mOnClickListeners.size() > 0) {
            int position = (Integer) view.getTag(R.id.edit_cell_position);
            int dataType = -1;
            switch (view.getId()) {
                case R.id.start_date:
                    dataType = START_DATE;
                    break;
                case R.id.start_time:
                    dataType = START_TIME;
                    break;
                case R.id.end_date:
                    dataType = END_DATE;
                    break;
                case R.id.end_time:
                    dataType = END_TIME;
                    break;
                case R.id.extra:
                    dataType = EXTRA_DATA;
                    break;
            }
            for (OnRowClickListener listener : mOnClickListeners) {
                listener.onRowDataClick(position, mRows.get(position), dataType);
            }
        }
    }
}
