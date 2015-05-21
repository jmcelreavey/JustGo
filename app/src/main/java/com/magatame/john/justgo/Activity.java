package com.magatame.john.justgo;

public class Activity {
    private int id, distance, calories, state;
    private String name, duration, date, startTime;

    // Get/Set Activity for use in populate history list view
    public Activity(int id, String name, String duration, int distance, int calories, int state, String date, String startTime) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.distance = distance;
        this.calories = calories;
        this.state = state;
        this.date = date;
        this.startTime = startTime;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDuration() {
        return duration;
    }

    public int getDistance() {
        return distance;
    }

    public int getCalories() {
        return calories;
    }

    public int getState() {
        return state;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }
}

