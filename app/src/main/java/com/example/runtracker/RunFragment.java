package com.example.runtracker;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.database.Cursor;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * A fragment representing a list of Items.
 */
public class RunFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    List<Run> runs;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RunFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RunFragment newInstance(int columnCount) {
        RunFragment fragment = new RunFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);

        NavController navController = NavHostFragment.findNavController(this);

        // Set the adapter
        Context context = view.getContext();
        RecyclerView recyclerView = view.findViewById(R.id.list);
        TextView emptyMessage = view.findViewById(R.id.emptyMessage); // Add reference to TextView
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Load run data
        runs = loadRuns();

        // If no runs found
        if (runs.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyMessage.setVisibility(View.GONE);
        }

        // Set adapter
        RViewAdapter adapter = new RViewAdapter(runs, getContext(), navController);
        recyclerView.setAdapter(adapter);

        // Setup swipe-to-delete
        setupSwipeToDelete(recyclerView);

        return view;
    }


    private List<Run> loadRuns() {
        List<Run> runList = new ArrayList<>();

        Cursor cursor = requireActivity().getContentResolver().query(
                RunContentProvider.CONTENT_URI,
                new String[]{"id", "date", "duration", "distance"},
                null,
                null,
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"));
                float distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance"));

                runList.add(new Run(id, date, duration, distance));
            }
            cursor.close();
        }

        return runList;
    }

    private void setupSwipeToDelete(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getBindingAdapterPosition();
                RViewAdapter adapter = (RViewAdapter) recyclerView.getAdapter();
                assert adapter != null;
                Run run = adapter.getItem(position);
                adapter.deleteRun(run.getRunID(), position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
}