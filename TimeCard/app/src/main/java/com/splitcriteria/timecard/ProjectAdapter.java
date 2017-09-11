package com.splitcriteria.timecard;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Deuce on 9/9/17.
 */

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    private String[] mProjects;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CardView mCardView;
        public ViewHolder(CardView v) {
            super(v);
            mCardView = v;
        }
    }

    public ProjectAdapter(String[] projects) {
        mProjects = projects;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView v = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_project, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ((TextView)holder.mCardView.findViewById(R.id.name))
                .setText(mProjects[position]);
    }

    @Override
    public int getItemCount() {
        return mProjects.length;
    }

}
