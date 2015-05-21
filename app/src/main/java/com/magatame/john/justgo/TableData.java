package com.magatame.john.justgo;

import android.provider.BaseColumns;

public class TableData {

    public TableData() {

    }

    //BaseColumns implements primary key ID, required by some console adapter classes
    public static abstract class TableInfo implements BaseColumns {

        //Column names - userDetails
        public static final String FULL_NAME = "full_name";
        public static final String AGE = "age";
        public static final String GENDER = "gender";
        public static final String WEIGHT = "weight";
        public static final String HEIGHT = "height";

        //Column names - history
        public static final String ID = "id";
        public static final String DURATION = "duration";
        public static final String DISTANCE = "distance";
        public static final String CALORIES = "calories";
        public static final String STATE = "state";
        public static final String DATE = "date";
        public static final String START_TIME = "start_time";

        //Database & Table names
        public static final String DATABASE_NAME = "justGo";
        public static final String TABLE_USER_DETAILS = "userDetails";
        public static final String TABLE_HISTORY = "history";
    }

    /* table index of the columns.
       Must match the order of creation for the table in class TableInfo
       This is to ensure the table index matches that of the enums */
    public enum UserDetailOrdinals {
        FULL_NAME, AGE, GENDER, WEIGHT, HEIGHT
    }

    public enum HistoryOrdinals {
        ID, DURATION, DISTANCE, CALORIES, STATE, DATE, START_TIME
    }
}
