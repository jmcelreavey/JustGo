package com.magatame.john.justgo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.magatame.john.justgo.TableData.TableInfo;

public class DatabaseOperations extends SQLiteOpenHelper {
    public static final int database_version = 1;

    //our query that will insert a table "userDetails" and the required columns
    public String CREATE_USER_DETAILS_QUERY = "CREATE TABLE " + TableInfo.TABLE_USER_DETAILS + "("
            + TableInfo.FULL_NAME + " STRING,"
            + TableInfo.AGE + " INT,"
            + TableInfo.GENDER + " BIT,"
            + TableInfo.WEIGHT + " Double,"
            + TableInfo.HEIGHT + " Double);";

    //our query that will insert a table "history" and the required columns
    public String CREATE_HISTORY_QUERY = "CREATE TABLE " + TableInfo.TABLE_HISTORY + "("
            + TableInfo.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TableInfo.DURATION + " STRING,"
            + TableInfo.DISTANCE + " INT,"
            + TableInfo.CALORIES + " INT,"
            + TableInfo.STATE + " INT,"
            + TableInfo.DATE + " STRING,"
            + TableInfo.START_TIME + " STRING);";

    public DatabaseOperations(Context context) {
        //Create our database
        super(context, TableInfo.DATABASE_NAME, null, database_version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //Run query to create table and columns
        sqLiteDatabase.execSQL(CREATE_USER_DETAILS_QUERY);
        sqLiteDatabase.execSQL(CREATE_HISTORY_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }

    public void putUserDetails(String name, int age, int gender, double weight, double height) {
        SQLiteDatabase SQL = getWritableDatabase();
        ContentValues cv = new ContentValues();

        // Object containing data to be inserted
        cv.put(TableInfo.FULL_NAME, name);
        cv.put(TableInfo.AGE, age);
        cv.put(TableInfo.GENDER, gender);
        cv.put(TableInfo.WEIGHT, weight);
        cv.put(TableInfo.HEIGHT, height);

        /* nullColumnHack (null) inserts null if nothing in content value
           Inserts our user details into the table */
        SQL.insert(TableInfo.TABLE_USER_DETAILS, null, cv);
    }

    // returns the table and all the requested columns content
    public Cursor getUserDetails() {
        SQLiteDatabase SQL = getReadableDatabase();
        String[] columns = {
                TableInfo.FULL_NAME,
                TableInfo.AGE,
                TableInfo.GENDER,
                TableInfo.WEIGHT,
                TableInfo.HEIGHT
        };

        // retrieve and return all columns in our table
        Cursor CR = SQL.query(TableInfo.TABLE_USER_DETAILS, columns, null, null, null, null, null);
        return CR;
    }

    /* updates user details using full name for the lookup,
       unique key isn't important as there will only ever be one entry in this table. */
    public void updateUserDetails(String name, String newName, int newAge, int newGender, double newWeight, double newHeight) {
        SQLiteDatabase SQL = getWritableDatabase();
        // our select string "where full name = passed in name"
        String selection = TableInfo.FULL_NAME + " = '" + name + "'";

        // The values being passed in for updating and there matching column.
        ContentValues cv = new ContentValues();
        cv.put(TableInfo.FULL_NAME, newName);
        cv.put(TableInfo.AGE, newAge);
        cv.put(TableInfo.GENDER, newGender);
        cv.put(TableInfo.WEIGHT, newWeight);
        cv.put(TableInfo.HEIGHT, newHeight);

        //perform update.
        SQL.update(TableInfo.TABLE_USER_DETAILS, cv, selection, null);
    }

    // Insert the recorded data into the DB.
    public void putHistory(String duration, int distance, int calories, int state, String date, String startTime) {
        SQLiteDatabase SQL = getWritableDatabase();
        ContentValues cv = new ContentValues();

        // Object containing data to be inserted
        cv.put(TableInfo.DURATION, duration);
        cv.put(TableInfo.DISTANCE, distance);
        cv.put(TableInfo.CALORIES, calories);
        cv.put(TableInfo.STATE, state);
        cv.put(TableInfo.DATE, date);
        cv.put(TableInfo.START_TIME, startTime);

        // nullColumnHack (null) inserts null if nothing in content value
        // Inserts our user details into the table
        SQL.insert(TableInfo.TABLE_HISTORY, null, cv);
    }

    // returns the table and all the requested columns content
    public Cursor getHistory() {
        SQLiteDatabase SQL = getReadableDatabase();
        String[] columns = {
                TableInfo.ID,
                TableInfo.DURATION,
                TableInfo.DISTANCE,
                TableInfo.CALORIES,
                TableInfo.STATE,
                TableInfo.DATE,
                TableInfo.START_TIME
        };

        //retrieve and return all columns in our table
        Cursor CR = SQL.query(TableInfo.TABLE_HISTORY, columns, null, null, null, null, null);
        return CR;
    }
}
