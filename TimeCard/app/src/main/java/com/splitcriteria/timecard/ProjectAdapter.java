package com.splitcriteria.timecard;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
 * A RecyclerView.Adapter which holds project names and displays them using
 * the card_project layout.
 */

class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> implements
        View.OnClickListener {

    interface OnProjectClickedListener {
        void onClockInOutClicked(String projectName);
        void onSettingsClicked(View view, String projectName);
    }

    private static final int UPDATE_SUMMARIES = 0;
    private static final long UPDATE_RATE_MILLIS = 1000;

    private Comparator<DataGroup> mIgnoreCaseComparator = new Comparator<DataGroup>() {
                @Override
                public int compare(DataGroup dg0, DataGroup dg1) {
                    return dg0.extendedMetadata.metadata.displayName.compareToIgnoreCase(
                            dg1.extendedMetadata.metadata.displayName);
                }
            };


    private static class DataGroup {
        ProjectData.ExtendedMetadata extendedMetadata;
        TextView summary;
    }

    private WeakReference<Context> mContextRef;
    private List<DataGroup> mProjects;
    private List<OnProjectClickedListener> mListeners = new ArrayList<>();

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView mCardView;
        TextView mProjectName;
        Button mClockInOut;
        TextView mSummary;
        Button mSettings;
        ViewHolder(CardView v) {
            super(v);
            mCardView = v;
            mProjectName = v.findViewById(R.id.name);
            mClockInOut = v.findViewById(R.id.project_clock_in_out);
            mSummary = v.findViewById(R.id.data_summary);
            mSettings = v.findViewById(R.id.project_settings);
        }
    }

    private static class SummaryUpdateHandler extends Handler {

        private WeakReference<ProjectAdapter> mAdapter;

        SummaryUpdateHandler(ProjectAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_SUMMARIES) {
                ProjectAdapter projectAdapter = mAdapter.get();
                if (projectAdapter != null) {
                    Context context = projectAdapter.mContextRef.get();
                    boolean updatedSummary = false;
                    if (context != null) {
                        // Refresh the metadata of the projects
                        ProjectData projectData = new ProjectData(context);
                        // Go through the data groups, check for projects which are clocked in
                        // and update the data summaries
                        for (DataGroup dataGroup : projectAdapter.mProjects) {
                            dataGroup.extendedMetadata = projectData.getProjectExtendedMetadata(
                                    dataGroup.extendedMetadata.metadata.projectName);
                            if (dataGroup.extendedMetadata.clockedIn && dataGroup.summary != null) {
                                dataGroup.summary.setText(dataGroup.extendedMetadata.dataSummary);
                                updatedSummary = true;
                            }
                        }
                        projectData.close(context);
                    }
                    // If there were data summary updates, then call this handler again
                    // if there's not another pending message waiting to be called
                    if (updatedSummary && !hasMessages(UPDATE_SUMMARIES)) {
                        sendEmptyMessageDelayed(UPDATE_SUMMARIES, UPDATE_RATE_MILLIS);
                    }
                }
            }
        }
    }

    private SummaryUpdateHandler mSummaryUpdateHandler = new SummaryUpdateHandler(this);

    ProjectAdapter(Context context) {
        mContextRef = new WeakReference<>(context);
        mProjects = new ArrayList<>();
        ProjectData projectData = new ProjectData(context);
        // Add both archived and non-archived projects to the project list
        DataGroup dataGroup;
        for (String projectName : projectData.getProjectNames(true)) {
            dataGroup = new DataGroup();
            dataGroup.extendedMetadata = projectData.getProjectExtendedMetadata(projectName);
            mProjects.add(dataGroup);
        }
        for (String projectName : projectData.getProjectNames(false)) {
            dataGroup = new DataGroup();
            dataGroup.extendedMetadata = projectData.getProjectExtendedMetadata(projectName);
            mProjects.add(dataGroup);
        }
        projectData.close(context);
        // Sort the list
        Collections.sort(mProjects, mIgnoreCaseComparator);
    }

    ProjectAdapter(Context context, boolean archived) {
        mContextRef = new WeakReference<>(context);
        mProjects = new ArrayList<>();
        ProjectData projectData = new ProjectData(context);
        // Add both archived and non-archived projects to the project list
        DataGroup dataGroup;
        for (String projectName : projectData.getProjectNames(archived)) {
            dataGroup = new DataGroup();
            dataGroup.extendedMetadata = projectData.getProjectExtendedMetadata(projectName);
            mProjects.add(dataGroup);
        }
        projectData.close(context);
        // Sort the list
        Collections.sort(mProjects, mIgnoreCaseComparator);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView root = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_project, parent, false);
        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DataGroup dataGroup = mProjects.get(position);
        dataGroup.summary = holder.mSummary;
        // If the summary update handler doesn't have any messages waiting for it
        // then add an update message
        if (dataGroup.extendedMetadata.clockedIn &&
                !mSummaryUpdateHandler.hasMessages(UPDATE_SUMMARIES)) {
            mSummaryUpdateHandler.sendEmptyMessageDelayed(UPDATE_SUMMARIES, UPDATE_RATE_MILLIS);
        }
        holder.mSummary.setText(dataGroup.extendedMetadata.dataSummary);
        holder.mProjectName.setText(dataGroup.extendedMetadata.metadata.displayName);
        holder.mClockInOut.setOnClickListener(this);
        holder.mClockInOut.setTag(R.id.project_name,
                                  dataGroup.extendedMetadata.metadata.projectName);
        holder.mSettings.setOnClickListener(this);
        holder.mSettings.setTag(R.id.project_name,
                                dataGroup.extendedMetadata.metadata.projectName);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        // This ViewHolder is being recycled, so remove the Summary text reference
        // from the DataGroup. This way, the SummaryUpdateHandler won't try to update
        // a recycled reference
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            DataGroup dataGroup = mProjects.get(position);
            dataGroup.summary = null;
        }
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    String getProjectName(int adapterPosition) {
        return adapterPosition < mProjects.size() ?
                mProjects.get(adapterPosition).extendedMetadata.metadata.projectName : null;
    }

    void remove(int adapterPosition) {
        mProjects.remove(adapterPosition);
        notifyItemRemoved(adapterPosition);
    }

    private int getPosition(String projectName) {
        for (int i = 0; i < mProjects.size(); i++) {
            DataGroup dataGroup = mProjects.get(i);
            if (dataGroup.extendedMetadata.metadata.projectName.equals(projectName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Forces the adapter to update a project's data from the Project database
     *
     * @param projectName   the project name
     * @return  true, if the update was successful
     */
    boolean updateProject(String projectName) {
        Context context = mContextRef.get();
        if (context == null) {
            throw new RuntimeException("Unable to update project adapter: no Context.");
        }
        int position = getPosition(projectName);
        if (position == -1) {
            return false;
        }
        ProjectData projectData = new ProjectData(context);
        mProjects.get(position).extendedMetadata =
                projectData.getProjectExtendedMetadata(projectName);
        projectData.close(context);
        notifyItemChanged(position);
        return true;
    }

    public void add(Context context, String projectName) {
        // Add the project name (list is now unsorted)
        ProjectData projectData = new ProjectData(context);
        DataGroup dataGroup = new DataGroup();
        dataGroup.extendedMetadata = projectData.getProjectExtendedMetadata(projectName);
        mProjects.add(dataGroup);
        projectData.close(context);
        // Sort the project names, ignoring the case
        Collections.sort(mProjects, mIgnoreCaseComparator);
        // Get the position of the newly inserted item
        int position = getPosition(projectName);
        // Notify listeners of the newly inserted item
        notifyItemInserted(position);
    }

    @Override
    public void onClick(View view) {
        // Get the project information based on the Project name (saved in the tag)
        String projectName = (String)view.getTag(R.id.project_name);
        int position = getPosition(projectName);
        DataGroup dataGroup = mProjects.get(position);
        int id = view.getId();
        if (id == R.id.project_clock_in_out) {
            for (OnProjectClickedListener listener : mListeners) {
                listener.onClockInOutClicked(projectName);
            }
        } else if (id == R.id.project_settings) {
            for (OnProjectClickedListener listener : mListeners) {
                listener.onSettingsClicked(view, projectName);
            }
        }
        // Re-pull the project's metadata after all the listeners have been
        // called (in case the metadata changed)
        Context context = mContextRef.get();
        if (context != null) {
            mSummaryUpdateHandler.sendEmptyMessageDelayed(UPDATE_SUMMARIES, UPDATE_RATE_MILLIS);
        }
    }

    void addOnProjectClickedListener(OnProjectClickedListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    void removeOnProjectClickedListener(OnProjectClickedListener listener) {
        mListeners.remove(listener);
    }
}