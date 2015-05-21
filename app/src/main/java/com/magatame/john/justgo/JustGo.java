package com.magatame.john.justgo;

import android.app.ActionBar;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

public class JustGo extends FragmentActivity implements TabListener, SensorEventListener {

    private CustomViewPager mViewPager;
    private ActionBar mActionBar;
    private SensorManager mSensorManager;
    private Sensor mSenAccelerometer;
    private int mAccelCounter;
    private long mLastUpdate = 0;
    private float mLastX, mLastY, mLastZ, mAccelSpeedAverage;
    private static final int WALKING_THRESHOLD = 100;
    private static final int RUNNING_THRESHOLD = 350;
    private static final int MALE = 1;

    public static int getMale() {
        return MALE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_just_go);

        mViewPager = (CustomViewPager) findViewById(R.id.pager); //Changes fragment based on user swiping
        mViewPager.setAdapter(new MyAdapter(getSupportFragmentManager())); //Gets the Fragment to be passed in
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                mActionBar.setSelectedNavigationItem(i); //If we swipe to a new fragment also set the tab to that fragment.
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        addTabs();

        //Gets user details from DB and checks if user exists.
        DatabaseOperations dbOp = new DatabaseOperations(getBaseContext());
        Cursor CR = dbOp.getUserDetails();
        if (!CR.moveToFirst()) {
            firstTimeUser(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, "4N28BDD6B33ZQR544CZ6");
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onStartSession(this, "4N28BDD6B33ZQR544CZ6");
    }

    public void startAccelerometerListener() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSenAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSenAccelerometer, 30000000);
    }

    public void stopAccelerometerListener() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.unregisterListener(this);
    }

    //adds the action bar tabs.
    public void addTabs() {
        mActionBar = getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.Tab tab1 = mActionBar.newTab(); //will be set as position 0
        tab1.setText("Go");
        tab1.setTabListener(this);

        ActionBar.Tab tab2 = mActionBar.newTab();
        tab2.setText("History");
        tab2.setTabListener(this);

        ActionBar.Tab tab3 = mActionBar.newTab();
        tab3.setText("Settings");
        tab3.setTabListener(this);

        mActionBar.addTab(tab1);
        mActionBar.addTab(tab2);
        mActionBar.addTab(tab3);
    }

    //if first time user, disable action bar/pager swipe, else enable them.
    public void firstTimeUser(boolean firstTime) {
        if (firstTime) {
            mViewPager.setCurrentItem(2);
            mActionBar.setSelectedNavigationItem(2);
            mViewPager.setPagingEnabled(false);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            Toast.makeText(this, "Please enter your details to continue", Toast.LENGTH_LONG).show();
        } else {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mViewPager.setCurrentItem(0);
            mActionBar.setSelectedNavigationItem(0);
            mViewPager.setPagingEnabled(true);
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition()); //Once we change tabs set the pager to the fragment position.

        // If we switch to the History Tab.
        if (tab.getPosition() == 1) {
            // Check if new data has been recorded since last update
            FragmentStatePagerAdapter a = (FragmentStatePagerAdapter) mViewPager.getAdapter();
            Go go = (Go) a.instantiateItem(mViewPager, 0);
            // Retrieve new records from DB and re-populate the list view.
            if (go.isHistoryUpdated()) {
                History history = (History) a.instantiateItem(mViewPager, 1);
                history.populateActivitiesList();
                history.populateListView();
                go.setHistoryUpdated(false);
            }
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onBackPressed() {
        //mViewPager.setCurrentItem(mPreviousPosition, true);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        TextView mStateTv;

        mStateTv = (TextView) findViewById(R.id.stateTv);
        if (mStateTv != null) {
            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];

                long curTime = System.currentTimeMillis();

                if ((curTime - mLastUpdate) > 100) {
                    long diffTime = (curTime - mLastUpdate);
                    mLastUpdate = curTime;

                    float speed = Math.abs(x + y + z - mLastX - mLastY - mLastZ) / diffTime * 10000;
                    mAccelSpeedAverage += speed;
                    mAccelCounter++;
                    //check for this first
                    if (mAccelCounter == 10) {
                        mAccelSpeedAverage = mAccelSpeedAverage / 10;
                        if (mAccelSpeedAverage > RUNNING_THRESHOLD) {
                            mStateTv.setText("Running");
                            mAccelCounter = 0;
                        } else if (mAccelSpeedAverage > WALKING_THRESHOLD) {
                            mStateTv.setText("Walking");
                            mAccelCounter = 0;
                        } else if (mAccelSpeedAverage < WALKING_THRESHOLD) {
                            mStateTv.setText("Idle");
                            mAccelCounter = 0;
                        }

                        mLastX = x;
                        mLastY = y;
                        mLastZ = z;
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

class MyAdapter extends FragmentStatePagerAdapter {

    MyAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        //sets/returns position of fragments:
        switch (i) {
            case 0:
                return new Go();
            case 1:
                return new History();
            case 2:
                return new Settings();
        }
        return null;
    }

    @Override
    public int getCount() {
        //How many pages exist:
        return 3;
    }
}

