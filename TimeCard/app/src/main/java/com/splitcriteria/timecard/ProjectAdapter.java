package com.splitcriteria.timecard;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
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

class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    private List<String> mProjects = new ArrayList<>();
    private Comparator<String> mIgnoreCaseStringComparator = new Comparator<String>() {
        @Override
        public int compare(String s, String t1) {
            return s.compareToIgnoreCase(t1);
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView mCardView;
        ViewHolder(CardView v) {
            super(v);
            mCardView = v;
        }
    }

    ProjectAdapter(String[] projects) {
        // Add all the project names
        mProjects.addAll(Arrays.asList(projects));
        // Sort them, ignoring the case
        Collections.sort(mProjects, mIgnoreCaseStringComparator);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView root = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_project, parent, false);
        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ((TextView)holder.mCardView.findViewById(R.id.name))
                .setText(mProjects.get(position));
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    String getProjectName(int adapterPosition) {
        return adapterPosition < mProjects.size() ? mProjects.get(adapterPosition) : null;
    }

    void remove(int adapterPosition) {
        mProjects.remove(adapterPosition);
        notifyItemRemoved(adapterPosition);
    }

    public void add(String projectName) {
        // Add the project name (list is now unsorted)
        mProjects.add(projectName);
        // Sort the project names, ignoring the case
        Collections.sort(mProjects, mIgnoreCaseStringComparator);
        // Get the position of the newly inserted item
        int position = mProjects.indexOf(projectName);
        // Notify listeners of the newly inserted item
        notifyItemInserted(position);
    }

}
