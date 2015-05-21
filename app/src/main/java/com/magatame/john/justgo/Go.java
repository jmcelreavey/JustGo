package com.magatame.john.justgo;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Go extends Fragment {
    private Button mRouteBtn, mStartStopBtn;
    private TextView mLongitudeTv, mLatitudeTv, mStartTimeTv, mCaloriesTv, mStateTv;
    private ImageView mTimeOfDay;
    private PopupWindow mPopupWindow;
    private static View sPopupView;
    private LocationManager mLocationManager;
    private double mLatitude, mLongitude;
    private GoogleMap mGoogleMap;
    private Chronometer mElapsedTime;
    private ArrayList<LatLng> mMapList = new ArrayList<LatLng>();
    private LocationListener mLocationListener;
    private Context mCtx;
    private DatabaseOperations mDbOp;
    private Time mNow;
    private int mTotalCalories, mLocationChangeCounter;
    private long mPreviousDuration;
    private float mAverageSpeed;
    private boolean mHistoryUpdated, mStartClicked, mLayoutVisible, mMapOpen, mSavedStateActivated;
    private String mStart, mStop;

    // Text views which contain text, used for setting visible/gone
    private TextView mStartTimeTextTv, mTimerTextTv, mLongitudeTextTv, mLatitudeTextTv,
            mStateTextTv, mCaloriesTextTv;

    // When new data is recorded set TRUE, ensures History gets the new data
    public void setHistoryUpdated(boolean historyUpdated) {
        this.mHistoryUpdated = historyUpdated;
    }

    // Used by main activity to check if History needs to retrieve the latest data
    public boolean isHistoryUpdated() {
        return mHistoryUpdated;
    }

    public Go() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mLatitude = location.getLatitude();
                mLongitude = location.getLongitude();

                //Every time a new location is found add it to our map list.
                LatLng newMapValue = new LatLng(mLatitude, mLongitude);
                mMapList.add(newMapValue);

                //blank string in order to cast double value to string
                mLatitudeTv.setText("" + mLatitude);
                mLongitudeTv.setText("" + mLongitude);

                //Used to work out calories burnt
                float speed = location.getSpeed();

                //Used to work out average state
                mLocationChangeCounter++;
                mAverageSpeed += speed;

                // Work out our mTotalCalories burnt
                Cursor CRUserDetails = mDbOp.getUserDetails();
                // Only set the values if there's data in the database.
                if (CRUserDetails.moveToFirst()) /* returns true if exists */ {
                    int weight = CRUserDetails.getInt(TableData.UserDetailOrdinals.WEIGHT.ordinal());
                    int height = CRUserDetails.getInt(TableData.UserDetailOrdinals.HEIGHT.ordinal());
                    int age = CRUserDetails.getInt(TableData.UserDetailOrdinals.AGE.ordinal());
                    int gender = (CRUserDetails.getInt(TableData.UserDetailOrdinals.GENDER.ordinal()));
                    long elapsedMillis = SystemClock.elapsedRealtime() - mElapsedTime.getBase();

                    // get the calories burnt since last check, add to total value and update text view
                    mTotalCalories += calculateCalories(gender, weight, height, age, elapsedMillis, speed);
                    mCaloriesTv.setText("" + mTotalCalories);
                }

                // Only process the map is it is open
                if (mMapOpen) {
                    processMap();
                }
                // Only set visible if the map contains locations
                if (!mMapList.isEmpty()) {
                    mRouteBtn.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save to instance state encase of rotation or loss of focus
        outState.putParcelableArrayList("map", mMapList);
        outState.putLong("duration", mElapsedTime.getBase());
        outState.putString("startTime", mStartTimeTv.getText().toString());
        outState.putString("longitude", mLongitudeTv.getText().toString());
        outState.putString("latitude", mLatitudeTv.getText().toString());
        outState.putString("calories", mCaloriesTv.getText().toString());
        outState.putInt("totalCalories", mTotalCalories);
        outState.putBoolean("started", mStartClicked);
        outState.putBoolean("visibility", mLayoutVisible);
        outState.putBoolean("historyUpdated", mHistoryUpdated);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // ensure instance state is not empty
        if (savedInstanceState != null) {
            mSavedStateActivated = true;

            /* Check if the start button had been pressed
               if so, restart it */
            if (savedInstanceState.getBoolean("started")) {
                startRecording();
            }

            // retrieve and repopulate the saved data
            mStartTimeTv.setText(savedInstanceState.getString("startTime"));
            mElapsedTime.setBase(savedInstanceState.getLong("duration"));
            mLongitudeTv.setText(savedInstanceState.getString("longitude"));
            mLatitudeTv.setText(savedInstanceState.getString("latitude"));
            mMapList = (savedInstanceState.<LatLng>getParcelableArrayList("map"));
            mCaloriesTv.setText(savedInstanceState.getString("calories"));
            mTotalCalories = savedInstanceState.getInt("totalCalories");
            mHistoryUpdated = savedInstanceState.getBoolean("historyUpdated");
            layoutVisibility(savedInstanceState.getBoolean("visibility"));

            mSavedStateActivated = false;
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_go, container, false);

        //initialise to allow the updating of visibility to gone/visible
        mStartTimeTextTv = (TextView) rootView.findViewById(R.id.startTimeTextTv);
        mTimerTextTv = (TextView) rootView.findViewById(R.id.timerTextTv);
        mLongitudeTextTv = (TextView) rootView.findViewById(R.id.longitudeTextTv);
        mLatitudeTextTv = (TextView) rootView.findViewById(R.id.latitudeTextTv);
        mStateTextTv = (TextView) rootView.findViewById(R.id.stateTextTv);
        mCaloriesTextTv = (TextView) rootView.findViewById(R.id.caloriesTextTv);

        // initialise to allow update data
        mLatitudeTv = (TextView) rootView.findViewById(R.id.latitudeTv);
        mLongitudeTv = (TextView) rootView.findViewById(R.id.longitudeTv);
        mStartTimeTv = (TextView) rootView.findViewById(R.id.startTimeTv);
        mCaloriesTv = (TextView) rootView.findViewById(R.id.caloriesTv);
        mStateTv = (TextView) rootView.findViewById(R.id.stateTv);
        mStartStopBtn = (Button) rootView.findViewById(R.id.startBtn); //Rename to startStopBtn
        mElapsedTime = (Chronometer) rootView.findViewById(R.id.timerCmr);
        mTimeOfDay = (ImageView) rootView.findViewById(R.id.timeOfDayIv);
        mCtx = rootView.getContext();
        mDbOp = new DatabaseOperations(mCtx);

        //Retrieve our string resources
        mStart = getResources().getString(R.string.start);
        mStop = getResources().getString(R.string.stop);

        //Set to current timezone rather than UTC
        mNow = new Time(Time.getCurrentTimezone());

        // Listen for start or stop press and call appropriate methods
        mStartStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check what text button is currently set as
                if (mStartStopBtn.getText().toString().contains(mStart)) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });
        mRouteBtn = (Button) rootView.findViewById(R.id.routeBtn);
        mRouteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMapOpen = true;
                inflatePopup();
                processMap();

                // Zoom in only on click not every refresh
                if (mGoogleMap != null) {
                    mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                }

                // listen for doneBtn (in Map fragment) click
                sPopupView.findViewById(R.id.doneBtn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMapOpen = false;
                        mPopupWindow.dismiss();
                        mGoogleMap.setMyLocationEnabled(false);
                    }
                });
            }
        });

        return rootView;
    }

    private void layoutVisibility(boolean visible) {
        // Array of our text views to avoid duplication
        TextView[] textViews = {mStartTimeTextTv, mTimerTextTv, mLongitudeTextTv, mLatitudeTextTv,
                mStateTextTv, mCaloriesTextTv, mStartTimeTv, mElapsedTime, mLongitudeTv, mLatitudeTv,
                mStateTv, mCaloriesTv};

        if (visible) {
            for (TextView textView : textViews) {
                textView.setVisibility(View.VISIBLE);
            }
            // Only set visible if the map contains locations
            if (!mMapList.isEmpty()) {
                mRouteBtn.setVisibility(View.VISIBLE);
            }
            mTimeOfDay.setVisibility(View.VISIBLE);
        } else {
            for (TextView textView : textViews) {
                textView.setVisibility(View.GONE);
            }
            // if the map is empty don't show it, else do.
            if (mMapList.isEmpty()) {
                mRouteBtn.setVisibility(View.GONE);
            } else {
                mRouteBtn.setVisibility(View.VISIBLE);
            }
            mTimeOfDay.setVisibility(View.GONE);
        }
    }

    public void startRecording() {
        // Set true encase onSaveInstance is required
        mStartClicked = true;
        mLayoutVisible = true;

        /* Wipe poly line from map, done on Start so that users can see there
           previous results up until starting a new one and hide button*/
        if (!mSavedStateActivated) {
            mMapList.clear();
            if (mGoogleMap != null) {
                mGoogleMap.clear();
            }
        }
        mSavedStateActivated = false;

        mRouteBtn.setVisibility(View.GONE);

        // Set our fields to visible
        layoutVisibility(mLayoutVisible);

        // Request location updates every 60 seconds
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, mLocationListener);

        // Set the start buttons text to be Stop
        mStartStopBtn.setText(mStop);

        // Initialise and Set up our data
        // Set to current time
        mNow.setToNow();

        // Hours:Minutes:Seconds
        mStartTimeTv.setText(mNow.format("%k:%M:%S"));

        // Start chronometer
        mElapsedTime.setBase(SystemClock.elapsedRealtime());
        mElapsedTime.start();

        // start Accelerometer listener
        ((JustGo) getActivity()).startAccelerometerListener();

        // Depending on our time of day, set the icon
        if (mNow.hour >= 0 && mNow.hour < 12) {
            mTimeOfDay.setImageResource(R.drawable.ic_morning_sun);
        } else if (mNow.hour >= 12 && mNow.hour < 18) {
            mTimeOfDay.setImageResource(R.drawable.ic_noon_sun);
        } else {
            mTimeOfDay.setImageResource(R.drawable.ic_night_moon);
        }
    }

    public void stopRecording() {
        // Set true encase onSaveInstance is required
        mStartClicked = false;
        mLayoutVisible = false;

        // Set our fields visibility to gone
        layoutVisibility(mLayoutVisible);

        // Works our elapsed time since starting in milliseconds
        long elapsedMillis = SystemClock.elapsedRealtime() - mElapsedTime.getBase();

        // set the duration in HH:MM:SS
        String duration = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(elapsedMillis),
                TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % TimeUnit.MINUTES.toSeconds(1));

        // sums the distance and rounds it up in metres
        int distance = (int) Math.round(sumDistance());

        // generates an average speed since recording started
        mAverageSpeed = mAverageSpeed / mLocationChangeCounter;

        // convert our speed from m/s to km/h
        double kmh = mAverageSpeed * 1 / 1000 * 3600;
        int state;

        if (Double.isNaN(kmh)) {
            state = -1;
        } else {
            if (kmh < 1) {
                // Idle
                state = 0;
            } else if (kmh < 6) {
                // Walking
                state = 1;
            } else if (kmh > 44.72) {
                // Usain Bolt's running record - user must be driving
                state = 2;
            } else {
                // Running
                state = 1;
            }
        }


        // Reset counter and average speed for next recording
        mAverageSpeed = 0;
        mLocationChangeCounter = 0;

        // Store our information
        mDbOp.putHistory(duration, distance, mTotalCalories, state, mNow.format("%d-%m-%Y"), mStartTimeTv.getText().toString());

        // Set history to true to ensure history fragment gets the latest entries
        mHistoryUpdated = true;

        // Reset calories and previous duration
        mTotalCalories = 0;
        mPreviousDuration = 0;
        mCaloriesTv.setText("" + mTotalCalories);

        // Reset Longitude and Latitude
        mLongitudeTv.setText(R.string.initialising);
        mLatitudeTv.setText(R.string.initialising);

        // Stop GPS, Accelerometer and Chronometer
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager = null;

        ((JustGo) getActivity()).stopAccelerometerListener();
        mElapsedTime.stop();

        mStartStopBtn.setText(mStart);
    }

    private double sumDistance() {
        // Location used for working out distance between
        Location loc = new Location("distance provider");

        double summedDistance = 0;
        double previousLatitude = -1;
        double previousLongitude = -1;

        // stores our distance between
        float[] results = new float[1];

        for (int i = 0; i < mMapList.size(); ++i) {
            LatLng latLng = mMapList.get(i);
            if (i == 0) {
                /* Previous weren't set yet, nothing to measure
                   Set them and skip loop */
                previousLatitude = latLng.latitude;
                previousLongitude = latLng.longitude;
                continue;
            }

            // Measure previous with current
            loc.distanceBetween(previousLatitude, previousLongitude, latLng.latitude, latLng.longitude, results);

            // Total the distance between all points
            // ~ array value 0 hold distance, 1 holds optional quickest path
            summedDistance += results[0];

            // Modify the previous ones
            previousLatitude = latLng.latitude;
            previousLongitude = latLng.longitude;
        }
        return summedDistance;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void inflatePopup() {
        // Check if a view already exists, if it does, remove it
        if (sPopupView != null) {
            ViewGroup parent = (ViewGroup) sPopupView.getParent();
            if (parent != null)
                parent.removeView(sPopupView);
        }
        try {
            // inflate popup
            sPopupView = getActivity().getLayoutInflater().inflate(R.layout.fragment_map, null);
        } catch (InflateException e) {
            /* map is already there, just return view as it is */
        }

        // initialise popupWindow for configuring
        mPopupWindow = new PopupWindow(sPopupView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // close popup on background touch
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), ""));
        mPopupWindow.setFocusable(false);
        mPopupWindow.setOutsideTouchable(true);

        // center popup
        mPopupWindow.showAtLocation(getActivity().findViewById(R.id.routeBtn), Gravity.CENTER, 0, 0);
        // set popup size
        mPopupWindow.update(0, 0, 800, 800);
    }

    private void processMap() {
            if (mGoogleMap == null) {
                mGoogleMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.mapId)).getMap();
            }
            if (mGoogleMap != null) {
                PolylineOptions polyOptions = new PolylineOptions();
                // for each map point recorded add it to the map line
                for (LatLng mapPoint : mMapList) {
                    polyOptions.add(mapPoint);
                }
                // set the line width and colour
                polyOptions.width(5).color(Color.GREEN).geodesic(true);

                // Enabling MyLocation Layer of Google Map
                mGoogleMap.setMyLocationEnabled(true);

                // Last map item for placing current location marker
                LatLng lastMapItem = mMapList.get(mMapList.size() - 1);

                // create the poly line, move camera to current location, zoom and set marker
                mGoogleMap.addPolyline(polyOptions);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(lastMapItem));

            }
        }

    private int calculateCalories(int gender, int weight, int height, int age, long duration, double speed) {
        //Convert the m/s to km/h
        double kmh = speed * 1 / 1000 * 3600;
        double met;
        long oldDuration;

        //Holds the previous duration for next check
        oldDuration = duration;

        // Use mPreviousDuration as it doesn't hold the current duration like oldDuration does
        if (mPreviousDuration != 0) {
            duration -= mPreviousDuration;
        }

        int seconds = (int) (duration * 0.001);

        // Work out the met - Metabolic Equations
        if (kmh < 2) {
            // Idle
            return 0;
        } else if (kmh > 2 && kmh < 2.7) {
            // walking, 1.2mph (2 km/h) - 1.7 mph (2.7 km/h), level ground, strolling, very slow
            met = 2.3;
        } else if (kmh <= 4) {
            // walking, 2.5 mph (4 km/h)
            met = 2.9;
        } else if (kmh <= 4.8) {
            // walking 3.0 mph (4.8 km/h)
            met = 3.3;
        } else if (kmh <= 5.5) {
            // walking 3.4 mph (5.5 km/h)
            met = 3.6;
        } else if (kmh <= 10) {
            // jogging
            met = 7.0;
        } else if (kmh <= 44.72) {
            // Running up to Usain Bolts speed
            met = 8.0;
        } else {
            //driving
            return 0;
        }

        double bmr;
        // Work out Basal Metabolic Rate based on gender
        if (gender == ((JustGo) getActivity()).getMale()) {
            bmr = (9.56 * weight) + (1.85 * height) - (4.68 * age) + 655;
        } else {
            bmr = (13.75 * weight) + (5 * height) - (6.76 * age) + 66;
        }

        // Used to equate how long you've been walking since last check
        mPreviousDuration = oldDuration;

        // Work out calories burnt from results
        int caloriesBurnt = ((int) Math.round((bmr / 24) * met * (seconds / 3600.0)));
        return caloriesBurnt;
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
