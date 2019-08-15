package com.linkme.fido.adapter;


import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MainTabAdapter extends RecyclerView.Adapter<MainTabAdapter.TabViewHolder> {

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class TabViewHolder extends RecyclerView.ViewHolder {
        public TabViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
