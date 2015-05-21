package com.magatame.john.justgo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class History extends Fragment {
    private List<Activity> mMyActivities = new ArrayList<Activity>();
    private Context ctx;
    private View view;
    private Activity currentActivity;

    public History() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_history, container, false);

        populateActivitiesList();
        populateListView();

        return view;
    }

    @Override
    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void populateActivitiesList() {
        // Reset this as we're retrieving an update
        mMyActivities = new ArrayList<Activity>();
        ctx = view.getContext();
        DatabaseOperations dbOp = new DatabaseOperations(ctx);

        // get the history table and user details
        Cursor CR = dbOp.getHistory();
        Cursor userDetailsCR = dbOp.getUserDetails();

        /* The cursor starts before the first result row, so on the first iteration
        this moves to the first result if it exists. If the cursor is empty, or the
        last row has already been processed, then the loop exits neatly. */
        while (CR.moveToNext()) {
            int id = CR.getInt(TableData.HistoryOrdinals.ID.ordinal());
            String duration = CR.getString(TableData.HistoryOrdinals.DURATION.ordinal());
            int distance = CR.getInt(TableData.HistoryOrdinals.DISTANCE.ordinal());
            int calories = CR.getInt(TableData.HistoryOrdinals.CALORIES.ordinal());
            int state = CR.getShort(TableData.HistoryOrdinals.STATE.ordinal());
            String date = CR.getString(TableData.HistoryOrdinals.DATE.ordinal());
            String startTime = CR.getString(TableData.HistoryOrdinals.START_TIME.ordinal());
            String name = "";
            if (userDetailsCR.moveToFirst()) {
                // insert name for sharing
                name = userDetailsCR.getString(TableData.UserDetailOrdinals.FULL_NAME.ordinal());
            }
            //add details to the activity
            mMyActivities.add(new Activity(id, name, duration, distance, calories, state, date, startTime));
        }
    }

    public void populateListView() {
        //populate list view
        ArrayAdapter<Activity> adapter = new ActivityListAdapter();
        ListView list = (ListView) view.findViewById(R.id.activityListView);
        list.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private class ActivityListAdapter extends ArrayAdapter<Activity> {

        public ActivityListAdapter() {
            super(view.getContext(), R.layout.item_view, mMyActivities);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Ensure view exists
            View itemView = convertView;
            if (itemView == null) {
                itemView = getActivity().getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            //Retrieve activity to inflate list with
            currentActivity = mMyActivities.get(position);

            //Populate Time of day Icon
            String[] separated = currentActivity.getStartTime().split(":");
            int recordedTime = Integer.parseInt(separated[0]); // this will contain the hour
            ImageView timeOfDay = (ImageView) itemView.findViewById(R.id.listItemTimeOfDayIc);

            // Morning
            if (recordedTime > 0 && recordedTime < 12) {
                timeOfDay.setImageResource(R.drawable.ic_morning_sun);
                //Mid-day
            } else if (recordedTime >= 12 && recordedTime < 18) {
                timeOfDay.setImageResource(R.drawable.ic_noon_sun);
                //Evening onwards
            } else {
                timeOfDay.setImageResource(R.drawable.ic_night_moon);
            }

            // Create share button
            ImageView shareItem = (ImageView) itemView.findViewById(R.id.shareIv);
            shareItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // on click share information
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, currentActivity.getName() + " has just travelled "
                            + currentActivity.getDistance() + "m" + " in " + currentActivity.getDuration() +
                            " and burnt " + currentActivity.getCalories() + " calories!");
                    startActivity(Intent.createChooser(shareIntent, "Share using"));
                }
            });

            TextView duration = (TextView) itemView.findViewById(R.id.listItemDurationTV);
            duration.setText("" + currentActivity.getDuration());

            TextView distance = (TextView) itemView.findViewById(R.id.listItemDistanceTV);
            distance.setText(currentActivity.getDistance() + "m");

            TextView calories = (TextView) itemView.findViewById(R.id.listItemCaloriesTV);
            calories.setText("" + currentActivity.getCalories());

            TextView state = (TextView) itemView.findViewById(R.id.listItemStateTV);
            if (currentActivity.getState() == -1) {
                // If the application is stopped too soon
                state.setText("Undefined");
            } else if (currentActivity.getState() == 0) {
                state.setText("Idle");
            } else if (currentActivity.getState() == 1) {
                state.setText("Walking");
            } else if (currentActivity.getState() == 2) {
                state.setText("Running");
            } else {
                state.setText("Driving");
            }

            TextView date = (TextView) itemView.findViewById(R.id.listItemDateTV);
            date.setText(currentActivity.getDate());

            TextView time = (TextView) itemView.findViewById(R.id.listItemTimeTV);
            time.setText(currentActivity.getStartTime());

            return itemView;
        }
    }

}
