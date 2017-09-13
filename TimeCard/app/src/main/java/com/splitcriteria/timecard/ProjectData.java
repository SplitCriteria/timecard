package com.splitcriteria.timecard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.ArrayMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

/**
 * Created by Deuce on 9/9/17.
 */

public class ProjectData {

    private ProjectDataOpenHelper mDBHelper;
    private SQLiteDatabase mDatabase;

    private static final int DATABASE_VERSION = 1;
    // Projects metadata table name and column names
    private static final String PROJECTS_TABLE = "projects";
    private static final String KEY_PROJECT_NAME = "name";
    private static final String KEY_ARCHIVED = "archived";
    private static final String KEY_TRACK_LOCATION = "track_location";
    private static final String KEY_CURRENT_TIMECARD = "current_timecard_row";
    // Project timecard table column names
    private static final String KEY_START_TIME = "start";
    private static final String KEY_END_TIME = "end";

    public class ProjectDataOpenHelper extends SQLiteOpenHelper {

        public ProjectDataOpenHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE " + PROJECTS_TABLE + " (" +
                                   KEY_PROJECT_NAME + " TEXT," +
                                   KEY_ARCHIVED + " INTEGER DEFAULT 0," +
                                   KEY_TRACK_LOCATION + " INTEGER DEFAULT 0," +
                                   KEY_CURRENT_TIMECARD + " INTEGER DEFAULT -1);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PROJECTS_TABLE + ";");
            onCreate(sqLiteDatabase);
        }
    }

    public ProjectData(Context context, String dbName) {
        mDBHelper = new ProjectDataOpenHelper(context, dbName);
        mDatabase = mDBHelper.getWritableDatabase();
    }

    public Map<String, Map<String, Object>> getProjectData() {
        Map<String, Map<String, Object>> results = new ArrayMap<>();
        Cursor cursor = mDatabase.rawQuery("SELECT * FROM " + PROJECTS_TABLE + ";", null);
        int nameIndex = cursor.getColumnIndex(KEY_PROJECT_NAME);
        int archivedIndex = cursor.getColumnIndex(KEY_ARCHIVED);
        int trackLocationIndex = cursor.getColumnIndex(KEY_TRACK_LOCATION);
        int currentTimecardIndex = cursor.getColumnIndex(KEY_CURRENT_TIMECARD);
        while (cursor.moveToNext()) {
            Map<String, Object> row = new HashMap<>();
            row.put(KEY_ARCHIVED, cursor.getInt(archivedIndex) != 0);
            row.put(KEY_TRACK_LOCATION, cursor.getInt(trackLocationIndex) != 0);
            row.put(KEY_CURRENT_TIMECARD, cursor.getInt(currentTimecardIndex));
            results.put(cursor.getString(nameIndex), row);
        }
        cursor.close();
        return results;
    }

    /**
     * @param archived
     * @return a list of projects names which are not archived
     */
    String[] getProjectNames(boolean archived) {
        Cursor cursor = mDatabase.rawQuery(
                "SELECT " + KEY_PROJECT_NAME + " " +
                "FROM " + PROJECTS_TABLE + " " +
                "WHERE " + KEY_ARCHIVED + "=" + (archived ? "1 " : "0 ") +
                "ORDER BY UPPER(" + KEY_PROJECT_NAME + ") ASC;", null);
        List<String> names = new ArrayList<>();
        int nameIndex = cursor.getColumnIndex(KEY_PROJECT_NAME);
        while (cursor.moveToNext()) {
            names.add(cursor.getString(nameIndex));
        }
        cursor.close();
        return names.toArray(new String[] {});
    }

    boolean exists(String project) {
        if (project == null) {
            return false;
        }
        Cursor cursor = mDatabase.rawQuery("SELECT " + KEY_PROJECT_NAME + " " +
                                           "FROM " + PROJECTS_TABLE + ";", null);
        List<String> names = new ArrayList<>();
        int nameIndex = cursor.getColumnIndex(KEY_PROJECT_NAME);
        while (cursor.moveToNext()) {
            names.add(cursor.getString(nameIndex));
        }
        cursor.close();
        return names.contains(project);
    }

    public boolean addProject(String project) {
        if (exists(project)) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(KEY_PROJECT_NAME, project);
        // Insert the project metadata
        long rowid = mDatabase.insert(PROJECTS_TABLE, null, cv);
        // Insert a new project table
        if (rowid != -1) {
            mDatabase.execSQL("DROP TABLE IF EXISTS '" + project + "';");
            mDatabase.execSQL("CREATE TABLE '" + project + "' (" +
                              KEY_START_TIME + " TEXT," +
                              KEY_END_TIME + " TEXT DEFAULT NULL);");
        }
        return rowid != -1;
    }

    /**
     * Archives or un-archives a project
     *
     * @param project   the project name
     * @param archive   true to archive, false to un-archive
     * @return
     */
    public boolean setArchived(String project, boolean archive) {
        if (exists(project)) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_ARCHIVED, archive ? 1 : 0);
            long updated = mDatabase.update(PROJECTS_TABLE, cv, KEY_PROJECT_NAME + "=?",
                                            new String[] {project});
            return updated > 0;
        } else {
            return false;
        }
    }

    /**
     * Deletes a project
     *
     * @param project
     * @return
     */
    public boolean deleteProject(String project) {
        if (exists(project)) {
            // Delete the project timecard table
            mDatabase.execSQL("DROP TABLE '" + project + "';");
            // Delete the metadata
            long updated = mDatabase.delete(PROJECTS_TABLE,
                                            KEY_PROJECT_NAME + "=?",
                                            new String[] {project});
            if (updated == 0) {
                throw new RuntimeException("Unable to delete timecard for '" + project + "'");
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isClockedIn(String project) {
        // Get the project data
        Map<String, Map<String, Object>> projectData = getProjectData();
        // If the project exists, then determine if it's checked in
        if (projectData.containsKey(project)) {
            Map<String, Object> data = projectData.get(project);
            // Checked in is TRUE if a row ID is not equal to -1 (invalid)
            return (Integer)data.get(KEY_CURRENT_TIMECARD) != -1;
        } else {
            return false;
        }
    }

    /**
     * Helper function which returns the current date time in an SQLite friendly format
     *
     * @return
     */
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * A project can only be clocked in once. A project must be clocked out before
     * another clock in is allowed.
     *
     * @param project
     * @return
     */
    public boolean clockIn(String project) {
        // Get the project data
        Map<String, Map<String, Object>> projectData = getProjectData();
        if (projectData.containsKey(project)) {
            Map<String, Object> data = projectData.get(project);
            // Make sure the project isn't already clocked in
            if ((Integer)data.get(KEY_CURRENT_TIMECARD) == -1) {
                // Add a new row (which has a default start timestamp) to the timecard
                ContentValues cv = new ContentValues();
                cv.put(KEY_START_TIME, getDateTime());
                long row_id = mDatabase.insert("'"+project+"'", null, cv);
                // Add the row_id to the project metadata
                cv = new ContentValues();
                cv.put(KEY_CURRENT_TIMECARD, (int)row_id);
                int updated = mDatabase.update(
                        PROJECTS_TABLE, cv,
                        KEY_PROJECT_NAME + "=?",
                        new String[] {project});
                if (updated == 0) {
                    throw new RuntimeException("Unable to update metadata for '" + project + "'");
                }
                return true;
            } else {
                // Project is already clocked in -- must be clocked out first
                return false;
            }
        } else {
            // Project does not exist
            return false;
        }
    }

    public boolean clockOut(String project) {
        // Get the project data
        Map<String, Map<String, Object>> projectData = getProjectData();
        if (projectData.containsKey(project)) {
            Map<String, Object> data = projectData.get(project);
            // Make sure the project is clocked in
            int row_id = (Integer)data.get(KEY_CURRENT_TIMECARD);
            if (row_id != -1) {
                // Add the stop time to the timecard
                ContentValues cv = new ContentValues();
                cv.put(KEY_END_TIME, getDateTime());
                int updated = mDatabase.update(
                        "'"+project+"'", cv, "ROWID=?",
                        new String[] {Integer.toString(row_id)});
                // If unable to update the row, then there was an error clocking out
                if (updated == 0) {
                    throw new RuntimeException("Unable to clock out of '" + project + "'");
                }
                // Update the metadata to show that we're clocked out
                cv = new ContentValues();
                cv.put(KEY_CURRENT_TIMECARD, -1);
                updated = mDatabase.update(
                        PROJECTS_TABLE, cv,
                        KEY_PROJECT_NAME + "=?",
                        new String[] {project});
                if (updated == 0) {
                    throw new RuntimeException("Unable to updated metadata for '" + project + "'");
                }
                return true;
            } else {
                // Project is not clocked in -- must be clocked in first
                return false;
            }
        } else {
            // Project does not exist
            return false;
        }
    }

    /**
     *
     * @param project
     * @return -1 if the project doesn't exist, >= 0 in seconds
     */
    public int getProjectTime(String project) {
        if (exists(project)) {
            int timeInSeconds = 0;
            // Get the elapsed time of all the timecard entries
            Cursor cursor = mDatabase.rawQuery(
                    "SELECT strftime('%s'," + KEY_END_TIME + ") - " +
                           "strftime('%s'," + KEY_START_TIME + ") AS 'time' " +
                    "FROM '" + project + "'" +
                    "WHERE " + KEY_END_TIME + " IS NOT NULL;", null);
            int timeIndex = cursor.getColumnIndex("time");
            while (cursor.moveToNext()) {
                timeInSeconds += cursor.getInt(timeIndex);
            }
            cursor.close();
            // Get the elapsed time of an ongoing timecard entry
            cursor = mDatabase.rawQuery(
                    "SELECT strftime('%s','" + getDateTime() + "') - " +
                           "strftime('%s'," + KEY_START_TIME + ") AS 'time' " +
                    "FROM '" + project + "'" +
                    "WHERE " + KEY_END_TIME + " IS NULL;", null);
            timeIndex = cursor.getColumnIndex("time");
            while (cursor.moveToNext()) {
                timeInSeconds += cursor.getInt(timeIndex);
            }
            cursor.close();
            return timeInSeconds;
        } else {
            // Project doesn't exist
            return -1;
        }
    }

    public void close() {
        mDBHelper.close();
    }
}
