package com.example.runtracker;
import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;


import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StartRun extends Fragment implements MapHandler.LocationUpdateListener {
    // For timer start/pause/stop
    private boolean running;
    private boolean wasRunning;

    // For UI
    private TextView timeView;
    private TextView distanceView;
    private TextView paceView;
    private Button startPauseButton;

    private MapHandler mapHandler;

    // For run
    Run currentRun = new Run(0, null, 0, 0.0f); // Initialize a temp run

    public StartRun() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ask for permissions
        // For map
        GPSTracker gpsTracker = new GPSTracker(requireContext());
        mapHandler = new MapHandler(this, gpsTracker);
        mapHandler.setLocationUpdateListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_start_run, container, false); // default code
        View view = inflater.inflate(R.layout.fragment_start_run, container, false);

        GPSTracker gpsTracker = new GPSTracker(requireContext());

        gpsTracker.getLocation();


        // Timers and buttons
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapHandler.initializeMap(mapFragment);
        }

        timeView = view.findViewById(R.id.timeView); // Get timeview resource

        distanceView = view.findViewById(R.id.distanceValueView);
        paceView = view.findViewById(R.id.averagePaceValueView);

        startPauseButton = view.findViewById(R.id.startPauseButton);
        Button resetButton = view.findViewById(R.id.resetButton);
        Button saveRunButton = view.findViewById(R.id.saveRunButton);

        saveRunButton.setOnClickListener(v -> saveRun());
        startPauseButton.setOnClickListener(v -> {
            if (running) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
        resetButton.setOnClickListener(v -> resetPage());
        runTimer();
        return view;
    }

    private void runTimer() {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                int hours = currentRun.getTotalSeconds() / 3600;
                int minutes = (currentRun.getTotalSeconds() % 3600) / 60;
                int secs = currentRun.getTotalSeconds() % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                        hours, minutes, secs);
                timeView.setText(time); // Change the time
                if (running) {
                    currentRun.setTotalSeconds(currentRun.getTotalSeconds() + 1); // Keep incrementing the seconds
                }
                handler.postDelayed(this, 1000); // Delay by a second each time
            }
        });
    }

    private void startTimer() {
        running = true;
        startPauseButton.setText(R.string.pause);
        MapHandler.enableLocationUpdates(); // Start location tracking to build user's route
    }

    private void pauseTimer() {
        running = false;
        startPauseButton.setText(R.string.resume);
        MapHandler.disableLocationUpdates();
    }
    // TO DO: Expand this method to be able to reset the whole page?
    // Need to implement the distance functionality first to finish the above to do
    private void resetTimer() {
        running = false;
        currentRun.setTotalSeconds(0);
        startPauseButton.setText(R.string.start);
        timeView.setText(R.string._00_00_00);
    }

    private void resetPage() {
        resetTimer();
        MapHandler.resetRoute();
        // Reset currentRun Object
        currentRun = new Run(0, null, 0, 0.0f);
    }

    @Override
    public void onPause() { // Add more functionality to the default pause to stop the timer as well
        super.onPause();
        wasRunning = running;
        running = false;
        MapHandler.disableLocationUpdates();
    }

    @Override
    public void onResume() { // Add more functionality to the default resume to resume the timer as well
        super.onResume();
        if (wasRunning) {
            running = true;
        }
        MapHandler.enableLocationUpdates();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("seconds", currentRun.getTotalSeconds());
        outState.putBoolean("running", running);
        outState.putBoolean("wasRunning", wasRunning);
    }

    public void saveRun() {

        pauseTimer();
        String currentDate = getCurrentDate();
        currentRun.setDate(currentDate);

        try (RunDatabaseHelper dbHelper = new RunDatabaseHelper(getContext())) {
            long runId = dbHelper.insertRun(currentRun); // Save run and get the generated run ID
            currentRun.setRunID(runId); // Set the run ID for the current run
            Log.d("saveRunDebug", "Run saved with ID: " + runId);

            // Step 2: Save the location points (route) associated with the run
            dbHelper.insertRunPoints(runId, MapHandler.getRoutePoints()); // Save the GPS points

            // Notify the user that the run has been saved successfully
            Toast.makeText(getContext(), "Run and route saved!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("saveRunError", "Error saving run: ", e);
            Toast.makeText(getContext(), "Failed to save run!", Toast.LENGTH_SHORT).show();
        }
    }



    private void setDistance(float distance) {
        // Converts the distance from meters to to miles and stores within the class
        float meterToMi = (float) (distance / 1609.344);
        float roundedDistance = (float) Math.round(meterToMi * 100) / 100f;
        currentRun.setDistance(roundedDistance); // The total distance has been updated
    }

    private String getCurrentDate() {
        // Implement date retrieval logic here
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onLocationUpdate(float distance) {
        // Can create separate methods just for changing the text

        // Defined in StartRun.java but used in MapHandler
        // When LocationUpdate are enabled, this is called on every update
        setDistance(distance);

        // Update average pace in this method instead of runTimer (Pace does not need to keep updating every second)
        paceView.setText(String.format("%.2f min/mi", currentRun.getAveragePace()));

        if (running) {
            String distanceText = String.format(Locale.getDefault(), "%.2f mi", distance / 1609.344);
            distanceView.setText(distanceText);
        }
    }
}