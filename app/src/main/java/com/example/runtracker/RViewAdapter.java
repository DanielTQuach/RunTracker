package com.example.runtracker;

import androidx.annotation.NonNull;

import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.runtracker.placeholder.PlaceholderContent.PlaceholderItem;
import com.example.runtracker.databinding.FragmentItemBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 */
public class RViewAdapter extends RecyclerView.Adapter<RViewAdapter.ViewHolder> {
    private final List<Run> mValues;
    private final Context mContext;
    private final NavController navController;

    public RViewAdapter(List<Run> items, Context context, NavController navController) {
        mValues = items;
        mContext = context;
        this.navController = navController;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Run run = mValues.get(position);

        // Set the ID
        holder.mIdView.setText("Run #" + run.getRunID());

        // Set the date, display "N/A" if null
        String date = run.getDate();

        // fetching date and splitting into date and time
        @SuppressLint("SimpleDateFormat") SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date parsedDate = null;
        try {
            parsedDate = inputFormat.parse(date);
        } catch (ParseException ignored) {}
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("MMM. dd, yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
        assert parsedDate != null;
        String datePart = dateFormat.format(parsedDate);
        String timePart = timeFormat.format(parsedDate);

        holder.mDateView.setText(!date.isEmpty() ? datePart + " at " + timePart : "N/A");

        // Set the distance
        holder.mDistanceView.setText(String.format(Locale.getDefault(), "Distance:\n %.2f mi", run.getDistance()));

        // Set the duration, display "00:00:00" if null or empty
        String formattedTime = run.getFormattedTotalTime();
        holder.mDurationView.setText(formattedTime != null && !formattedTime.isEmpty() ? "Time:\n " + formattedTime : "Time: 00:00:00");

        holder.itemContent.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("runId", run.getRunID());

            navController.navigate(R.id.action_runFragment_to_detailFragment, args);
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mDateView;
        public final TextView mDistanceView;
        public final TextView mDurationView;
        public final View itemContent;

        public ViewHolder(FragmentItemBinding binding) {
            super(binding.getRoot());
            mIdView = binding.runIdText;
            mDateView = binding.dateText;
            mDistanceView = binding.distanceText;
            mDurationView = binding.durationText;
            itemContent = binding.itemContent;

        }
    }

    public Run getItem(int position) {
        return mValues.get(position);
    }

    public void deleteRun(long runId, int position) {
        Uri uri = ContentUris.withAppendedId(RunContentProvider.CONTENT_URI, runId);
        int deletedRows = mContext.getContentResolver().delete(uri, null, null);

        if (deletedRows > 0) {
            mValues.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mValues.size());
        } else {
            // Handle deletion failure
            Toast.makeText(mContext, "Failed to delete run", Toast.LENGTH_SHORT).show();
        }
    }

}