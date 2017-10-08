package com.splitcriteria.timecard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.ArrayMap;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Timecard - Allows users to easily track time-based data for analysis.
 * Copyright (C) 2017  Nicholas Johnson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Timecard - Copyright (C) 2017  Nicholas Johnson
 *
 * This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
 * This is free software, and you are welcome to redistribute it
 * under certain conditions; type `show c' for details.
 *
 * Contains methods for access project data which is backed by an SQLite database.
 * Because this class reserves the database resource using DatabaseLock, the client
 * should use close(Context) immediately after finished using the methods within.
 */

class ProjectData {

    private ProjectDataOpenHelper mDBHelper;
    private SQLiteDatabase mDatabase;
    private WeakReference<Context> mContextRef;

    private static final int DATABASE_VERSION = 3;
    // Projects metadata table name and column names
    private static final String PROJECTS_TABLE = "projects";
    private static final String KEY_PROJECT_NAME = "name";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_ARCHIVED = "archived";
    private static final String KEY_TRACK_LOCATION = "track_location";
    private static final String KEY_NO_DURATION = "no_duration";
    private static final String KEY_USES_EXTRA_DATA = "use_extra";
    private static final String KEY_EXTRA_DATA_TITLE = "extra_data_title";
    private static final String KEY_DEFAULT_EXTRA_DATA = "default_extra";
    private static final String KEY_DATA_SUMMARY_METHOD = "data_summary_method";
    private static final String KEY_SUPPRESS_NOTIFICATION = "suppress_notification";
    private static final String KEY_CURRENT_TIMECARD = "current_timecard_row";
    // Project timecard table column names
    private static final String KEY_START_TIME = "start";
    private static final String KEY_END_TIME = "end";
    private static final String KEY_EXTRA_DATA = "extra";
    private static final String[] PROJECT_COLUMNS =
            new String[] {"rowid", KEY_START_TIME, KEY_END_TIME, KEY_EXTRA_DATA};

    class Metadata {
        boolean archived;
        boolean trackLocation;
        boolean noDuration;
        boolean suppressNotification;
        boolean usesExtraData;
        String extraDataTitle;
        String projectName;
        String displayName;
        String defaultExtraData;
        int currentTimecard;
        String dataSummaryMethod;
    }

    class ExtendedMetadata {
        Metadata metadata;

        int totalTime;
        int metadataTime;
        boolean clockedIn;
        String dataSummary;
    }

    class Row {
        int id;
        Calendar startTime;
        Calendar endTime;
        String extraData;
    }

    private class ProjectDataOpenHelper extends SQLiteOpenHelper {

        ProjectDataOpenHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            Context context = mContextRef.get();
            if (context == null) {
                throw new RuntimeException("Missing Context: unable to upgrade database.");
            }
            String defaultDataSummary =
                    "'" + context.getString(R.string.preferences_summary_type_default_value) + "'";
            sqLiteDatabase.execSQL("CREATE TABLE " + PROJECTS_TABLE + " (" +
                                   KEY_PROJECT_NAME + " TEXT," +
                                   KEY_DISPLAY_NAME + " TEXT," +
                                   KEY_ARCHIVED + " INTEGER DEFAULT 0," +
                                   KEY_TRACK_LOCATION + " INTEGER DEFAULT 0," +
                                   KEY_NO_DURATION + " INTEGER DEFAULT 0," +
                                   KEY_SUPPRESS_NOTIFICATION + " INTEGER DEFAULT 0," +
                                   KEY_USES_EXTRA_DATA + " INTEGER DEFAULT 0," +
                                   KEY_EXTRA_DATA_TITLE + " TEXT," +
                                   KEY_DEFAULT_EXTRA_DATA + " TEXT DEFAULT NULL," +
                                   KEY_DATA_SUMMARY_METHOD + " TEXT DEFAULT " +
                                        defaultDataSummary + "," +
                                   KEY_CURRENT_TIMECARD + " INTEGER DEFAULT -1);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            Context context = mContextRef.get();
            if (context == null) {
                throw new RuntimeException("Missing Context: unable to upgrade database.");
            }
            switch (oldVersion) {
                case 1:
                    sqLiteDatabase.execSQL("ALTER TABLE " + PROJECTS_TABLE + " ADD COLUMN " +
                            KEY_DATA_SUMMARY_METHOD + " TEXT DEFAULT '" +
                            context.getString(R.string.preferences_summary_type_default_value) +
                            "';");
                case 2:
                    sqLiteDatabase.execSQL("ALTER TABLE " + PROJECTS_TABLE + " ADD COLUMN " +
                            KEY_DISPLAY_NAME + " TEXT;");
                    sqLiteDatabase.execSQL("ALTER TABLE " + PROJECTS_TABLE + " ADD COLUMN " +
                            KEY_SUPPRESS_NOTIFICATION + " INTEGER DEFAULT 0;");
                    sqLiteDatabase.execSQL("ALTER TABLE " + PROJECTS_TABLE + " ADD COLUMN " +
                            KEY_EXTRA_DATA_TITLE + " TEXT;");

            }
        }
    }

    /**
     * Creates a writable database using the applications default database filename
     *
     * @param context   a valid context
     */
    ProjectData(Context context) {
        mContextRef = new WeakReference<>(context);
        // Make sure a lock can be acquired
        if (DatabaseLock.acquire(context, DatabaseLock.DATABASE)) {
            mDBHelper = new ProjectDataOpenHelper(
                    context, context.getString(R.string.default_database_filename));
            mDatabase = mDBHelper.getWritableDatabase();
        }
    }

    ProjectData(Context context, String dbName) {
        mContextRef = new WeakReference<>(context);
        if (DatabaseLock.acquire(context, DatabaseLock.DATABASE)) {
            mDBHelper = new ProjectDataOpenHelper(context, dbName);
            mDatabase = mDBHelper.getWritableDatabase();
        }
    }

    private Map<String, Metadata> getMetadata() {
        Map<String, Metadata> results = new ArrayMap<>();
        Cursor cursor = mDatabase.rawQuery("SELECT * FROM " + PROJECTS_TABLE + ";", null);
        int nameIndex = cursor.getColumnIndex(KEY_PROJECT_NAME);
        int displayNameIndex = cursor.getColumnIndex(KEY_DISPLAY_NAME);
        int archivedIndex = cursor.getColumnIndex(KEY_ARCHIVED);
        int trackLocationIndex = cursor.getColumnIndex(KEY_TRACK_LOCATION);
        int suppressNotificationIndex = cursor.getColumnIndex(KEY_SUPPRESS_NOTIFICATION);
        int currentTimecardIndex = cursor.getColumnIndex(KEY_CURRENT_TIMECARD);
        int usesExtraDataIndex = cursor.getColumnIndex(KEY_USES_EXTRA_DATA);
        int extraDataTitleIndex = cursor.getColumnIndex(KEY_EXTRA_DATA_TITLE);
        int defaultExtraDataIndex = cursor.getColumnIndex(KEY_DEFAULT_EXTRA_DATA);
        int noDurationIndex = cursor.getColumnIndex(KEY_NO_DURATION);
        int dataSummaryIndex = cursor.getColumnIndex(KEY_DATA_SUMMARY_METHOD);
        while (cursor.moveToNext()) {
            Metadata metadata = new Metadata();
            metadata.projectName = cursor.getString(nameIndex);
            metadata.displayName = cursor.getString(displayNameIndex);
            // If the display name is not set, then use the project name
            if (TextUtils.isEmpty(metadata.displayName)) {
                metadata.displayName = metadata.projectName;
            }
            metadata.archived = cursor.getInt(archivedIndex) != 0;
            metadata.trackLocation = cursor.getInt(trackLocationIndex) != 0;
            metadata.suppressNotification = cursor.getInt(suppressNotificationIndex) != 0;
            metadata.currentTimecard = cursor.getInt(currentTimecardIndex);
            metadata.usesExtraData = cursor.getInt(usesExtraDataIndex) != 0;
            metadata.extraDataTitle = cursor.getString(extraDataTitleIndex);
            metadata.defaultExtraData = cursor.getString(defaultExtraDataIndex);
            metadata.noDuration = cursor.getInt(noDurationIndex) != 0;
            metadata.dataSummaryMethod = cursor.getString(dataSummaryIndex);
            results.put(metadata.projectName, metadata);
        }
        cursor.close();
        return results;
    }

    /**
     * Returns the Metadata for a specific project
     *
     * @param projectName   a project name
     * @return  Metadata for the project, or null if the project name doesn't exist
     */
    Metadata getProjectMetadata(String projectName) {
        return projectName != null ? getMetadata().get(projectName) : null;
    }

    /**
     * Returns extended metadata for a project
     *
     * @param projectName   a project name
     * @return  ExtendedMetadata for a project
     */
    ExtendedMetadata getProjectExtendedMetadata(String projectName) {
        Metadata metadata = getMetadata().get(projectName);
        if (metadata != null) {
            ExtendedMetadata extendedMetadata = new ExtendedMetadata();
            extendedMetadata.metadata = metadata;
            extendedMetadata.clockedIn = isClockedIn(projectName);
            List<Row> rowData = getRows(projectName);
            extendedMetadata.dataSummary = DataSummary.getSummary(
                    mContextRef.get(), extendedMetadata.metadata.dataSummaryMethod, rowData);
            extendedMetadata.totalTime = 0;
            Calendar now = Calendar.getInstance(Locale.getDefault());
            for (Row row : rowData) {
                Calendar endTime = row.endTime == null ? now : row.endTime;
                extendedMetadata.totalTime +=
                        (int)((endTime.getTimeInMillis()-row.startTime.getTimeInMillis())/1000);
            }
            extendedMetadata.metadataTime = (int)(now.getTimeInMillis() / 1000);
            return extendedMetadata;
        } else {
            return null;
        }
    }

    /**
     * Gets a list of project names (either archived or not archived)
     *
     * @param archived  flag to return archived or not archived projects
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
        // Add the protected metadata table name (so it isn't overwritten)
        names.add(PROJECTS_TABLE);
        int nameIndex = cursor.getColumnIndex(KEY_PROJECT_NAME);
        while (cursor.moveToNext()) {
            names.add(cursor.getString(nameIndex));
        }
        cursor.close();
        return names.contains(project);
    }

    /**
     * Sets the value of a project's metadata to a specific value
     *
     * @param projectName the project name
     * @param column_key the name of the column
     * @param value the value, either: Integer, Long, Boolean (stored as 1 or 0), or String
     * @return  true, if the update was successful
     */
    private boolean setMetadataValue(String projectName, String column_key, @NonNull Object value) {
        Metadata metadata = getProjectMetadata(projectName);
        if (metadata != null) {
            ContentValues cv = new ContentValues();
            if (value instanceof Integer) {
                cv.put(column_key, (Integer)value);
            } else if (value instanceof Long) {
                cv.put(column_key, (Long)value);
            } else if (value instanceof Boolean) {
                // Boolean values are stored as integers in the SQLite database
                cv.put(column_key, (Boolean)value ? 1 : 0);
            } else if (value instanceof String) {
                cv.put(column_key, (String)value);
            }
            // Update the database
            int updated = mDatabase.update(
                    PROJECTS_TABLE, cv,
                    KEY_PROJECT_NAME + "=?",
                    new String[]{projectName});
            if (updated == 0) {
                throw new RuntimeException("Unable to update metadata for '" + projectName + "'");
            }
            return true;
        } else {
            // Project doesn't exist
            return false;
        }
    }

    /**
     * Updates metadata for a project. Note: this function will not update the currentTimecard
     * as this is strictly controlled with clockIn() and clockOut()
     *
     * @param project   project name
     * @param newMetadata   an updated metadata object
     * @return  true, if the update was successful
     */
    boolean updateMetadata(String project, @NonNull Metadata newMetadata) {
        Metadata prevMetadata = getProjectMetadata(project);
        if (prevMetadata != null) {
            // Change the display name
            if (TextUtils.isEmpty(prevMetadata.displayName) ||
                    !prevMetadata.displayName.equals(newMetadata.displayName)) {
                setMetadataValue(project, KEY_DISPLAY_NAME, newMetadata.displayName);
            }
            // Change the metadata properties which are different
            if (prevMetadata.noDuration != newMetadata.noDuration) {
                setMetadataValue(project, KEY_NO_DURATION, newMetadata.noDuration);
            }
            if (prevMetadata.usesExtraData != newMetadata.usesExtraData) {
                setMetadataValue(project, KEY_USES_EXTRA_DATA, newMetadata.usesExtraData);
            }
            if (prevMetadata.defaultExtraData == null ||
                    newMetadata.defaultExtraData == null ||
                    !prevMetadata.defaultExtraData.equals(newMetadata.defaultExtraData)) {
                // Catch any null default extra data and set it to an empty string
                if (newMetadata.defaultExtraData == null) {
                    newMetadata.defaultExtraData = "";
                }
                setMetadataValue(project, KEY_DEFAULT_EXTRA_DATA, newMetadata.defaultExtraData);
            }
            if (prevMetadata.archived != newMetadata.archived) {
                setMetadataValue(project, KEY_ARCHIVED, newMetadata.archived);
            }
            if (prevMetadata.trackLocation != newMetadata.trackLocation) {
                setMetadataValue(project, KEY_TRACK_LOCATION, newMetadata.trackLocation);
            }
            if (prevMetadata.suppressNotification != newMetadata.suppressNotification) {
                setMetadataValue(project, KEY_SUPPRESS_NOTIFICATION,
                                 newMetadata.suppressNotification);
            }
            if (!prevMetadata.dataSummaryMethod.equals(newMetadata.dataSummaryMethod)) {
                setMetadataValue(project, KEY_DATA_SUMMARY_METHOD, newMetadata.dataSummaryMethod);
            }
            if (TextUtils.isEmpty(prevMetadata.extraDataTitle) ||
                    !prevMetadata.extraDataTitle.equals(newMetadata.extraDataTitle)) {
                setMetadataValue(project, KEY_EXTRA_DATA_TITLE, newMetadata.extraDataTitle);
            }
            return true;
        } else {
            // Project doesn't exist
            return false;
        }
    }

    boolean addProject(String project) {
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
                              KEY_END_TIME + " TEXT DEFAULT NULL," +
                              KEY_EXTRA_DATA + " TEXT DEFAULT '');");
        }
        return rowid != -1;
    }

    /**
     * Renames a project
     *
     * @param projectName    the project's name
     * @param displayName    the new display name to be used
     * @return  true, if the rename was successful
     */
    boolean renameProject(String projectName, String displayName) {
        // Rename a project by altering its display name
        return exists(projectName) && setMetadataValue(projectName, KEY_DISPLAY_NAME, displayName);
    }

    /**
     * Archives or un-archives a project. Convenience method for updateMetadata() with
     * the archive flag set.
     *
     * @param project   the project name
     * @param archive   true to archive, false to un-archive
     * @return  true if setting the archive flag was successful
     */
    boolean setArchived(String project, boolean archive) {
        Metadata metadata = getProjectMetadata(project);
        if (metadata != null) {
            metadata.archived = archive;
            return updateMetadata(project, metadata);
        } else {
            return false;
        }
    }

    /**
     * Deletes a project
     *
     * @param project project name
     *
     * @return  true if the project was deleted
     */
    boolean deleteProject(String project) {
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

    /**
     * Returns true if the project is clocked in (i.e. waiting to be clocked out)
     *
     * @param project   project name
     * @return true, if clocked in
     */
    boolean isClockedIn(String project) {
        // Get the project data
        Metadata metadata = getProjectMetadata(project);
        // Checked in is TRUE if a row ID is not equal to -1 (invalid)
        return metadata != null && metadata.currentTimecard != -1;
    }

    /**
     * Helper function which returns the current date time in an SQLite friendly format
     * yyyy-MM-dd HH:mm:ss
     *
     * @param date  a Date to convert to a date time format, or null to get a current time
     * @return  a String representing the current timestamp (year, month, date, and time)
     */
    private String getDateTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        if (date == null) {
            date = new Date();
        }
        return dateFormat.format(date);
    }

    /**
     * Helper function which returns a Date in the default locale based on a String
     * returned from getDateTime()
     *
     * @param datetime  a time String from getDateTime() -- format: "yyyy-MM-dd HH:mm:ss"
     * @return  a Date object, or null if there was a ParseException
     */
    private Date getDate(String datetime) {
        if (TextUtils.isEmpty(datetime)) {
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            return dateFormat.parse(datetime);
        } catch (ParseException exception) {
            return null;
        }
    }

    /**
     * Gets the rows for a specific project
     *
     * @param project   the project name
     * @return  the project's row data, or null if the project doesn't exist
     */
    List<Row> getRows(String project) {
        if (exists(project)) {
            List<Row> results = new ArrayList<>();
            Cursor cursor = mDatabase.query(project, PROJECT_COLUMNS, null, null, null, null,
                                            KEY_START_TIME + " DESC");
            int rowIdIndex = cursor.getColumnIndex("rowid");
            int startIndex = cursor.getColumnIndex(KEY_START_TIME);
            int endIndex = cursor.getColumnIndex(KEY_END_TIME);
            int extraIndex = cursor.getColumnIndex(KEY_EXTRA_DATA);
            while (cursor.moveToNext()) {
                Row row = new Row();
                row.id = cursor.getInt(rowIdIndex);
                row.startTime = Calendar.getInstance(Locale.getDefault());
                row.startTime.setTime(getDate(cursor.getString(startIndex)));
                // While it's not possible to have a missing start time, there
                // could be a NULL end time if the project is clocked in
                Date endDate = getDate(cursor.getString(endIndex));
                if (endDate != null) {
                    row.endTime = Calendar.getInstance(Locale.getDefault());
                    row.endTime.setTime(getDate(cursor.getString(endIndex)));
                } else {
                    row.endTime = null;
                }
                row.extraData = cursor.getString(extraIndex);
                results.add(row);
            }
            cursor.close();
            return results;
        } else {
            return null;
        }
    }

    /**
     * Updates a row of a given project
     *
     * @param project   a project name
     * @param row       a Row object
     * @return  true, if the row was updated; false if the project doesn't exist
     *          or the row update failed
     */
    boolean updateRow(String project, Row row) {
        if (exists(project)) {
            ContentValues cv =  new ContentValues();
            cv.put(KEY_START_TIME, getDateTime(row.startTime.getTime()));
            cv.put(KEY_END_TIME, getDateTime(row.endTime.getTime()));
            cv.put(KEY_EXTRA_DATA, row.extraData);
            int updated = mDatabase.update(project, cv, "rowid=?",
                                           new String[] {Integer.toString(row.id)});
            return updated != 0;
        } else {
            return false;
        }
    }

    /**
     * Deletes a row from a specific project
     *
     * @param project    a project name
     * @param id         a row id of a project's entry
     * @return  true, if the row was deleted
     */
    boolean deleteRow(String project, int id) {
        if (exists(project)) {
            int deleted = mDatabase.delete(project, "rowid=?",
                                           new String[] {Integer.toString(id)});
            return deleted != 0;
        } else {
            return false;
        }
    }

    /**
     * A project can only be clocked in once. A project must be clocked out before
     * another clock in is allowed. Projects which are set to "no duration" do not
     * clock in. Instead, a new row of data is inserted using the same start and
     * end time.
     *
     * If the project has "use extra data" flag set, then the specified extra data
     * is used. Or, if null is passed, then the default (if any) is used.
     *
     * @param project project name
     * @param extra specific extra data to use, or null to use the default extra data
     *
     * @return  true, if the project was clocked in
     */
    boolean clockIn(String project, String extra) {
        // Get the project data
        Metadata metadata = getProjectMetadata(project);
        if (metadata != null) {
            // Make sure the project isn't already clocked in
            if (metadata.currentTimecard == -1) {
                // Add a new row to the timecard
                ContentValues cv = new ContentValues();
                // Get the current timestamp
                String dateTime = getDateTime(null);
                cv.put(KEY_START_TIME, dateTime);
                // If this project is labeled as "no duration" then set the end time too
                if (metadata.noDuration) {
                    cv.put(KEY_END_TIME, dateTime);
                }
                // If the project is uses extra data, then insert it
                if (metadata.usesExtraData || extra != null) {
                    if (extra != null) {
                        cv.put(KEY_EXTRA_DATA, extra);
                    } else if (metadata.defaultExtraData != null) {
                        cv.put(KEY_EXTRA_DATA, metadata.defaultExtraData);
                    }
                }
                // Insert the new data
                long row_id = mDatabase.insert("'"+project+"'", null, cv);
                // If "no duration" flag is not set (default), then save the row id of the
                // incomplete row, to signify that the project is "clocked in"
                if (!metadata.noDuration) {
                    setMetadataValue(project, KEY_CURRENT_TIMECARD, row_id);
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

    /**
     * Clock a project out and return the duration of the event
     *
     * @param project   the project name
     * @return  the time of the most recent duration, or -1 if clock out failed
     *          (e.g. the project isn't clocked in, or doesn't exist)
     */
    int clockOut(String project) {
        // Get the project data
        Metadata metadata = getProjectMetadata(project);
        if (metadata != null) {
            // Make sure the project is clocked in
            if (metadata.currentTimecard != -1) {
                // Add the stop time to the timecard
                ContentValues cv = new ContentValues();
                cv.put(KEY_END_TIME, getDateTime(null));
                int rowID = metadata.currentTimecard;
                int updated = mDatabase.update(
                        "'"+project+"'", cv, "rowid=?",
                        new String[] {Integer.toString(rowID)});
                // If unable to update the row, then there was an error clocking out
                if (updated == 0) {
                    throw new RuntimeException("Unable to clock out of '" + project + "'");
                }
                // Update the metadata to show that we're clocked out
                setMetadataValue(project, KEY_CURRENT_TIMECARD, -1);
                // Get the time of the most recent event
                int timeInSeconds = 0;
                // Get the elapsed time of all the timecard entries
                Cursor cursor = mDatabase.rawQuery(
                        "SELECT strftime('%s'," + KEY_END_TIME + ") - " +
                               "strftime('%s'," + KEY_START_TIME + ") AS 'time' " +
                        "FROM '" + project + "'" +
                        "WHERE rowid=?;",
                        new String[] {Integer.toString(rowID)});
                int timeIndex = cursor.getColumnIndex("time");
                while (cursor.moveToNext()) {
                    timeInSeconds += cursor.getInt(timeIndex);
                }
                cursor.close();
                return timeInSeconds;
            } else {
                // Project is not clocked in -- must be clocked in first
                return -1;
            }
        } else {
            // Project does not exist
            return -1;
        }
    }

    boolean toggleClockInOut(String project, String extraData) {
        // Attempt to clock the project in -- it's either already clocked in
        // and will return false, or doesn't exist and will return false
        if (!clockIn(project, extraData)) {
            // Attempt to clock the project out. We know it's already clocked
            // in or doesn't exist. Return the result of clocking out which is
            // either success clocking out, or false if an error (i.e. the project
            // doesn't exist or there was a problem with the database).
            return clockOut(project) != -1;
        }
        // The project was able to be clocked in -- return success
        return true;
    }

    /**
     * Returns the amount of time the project has been clocked in over its lifetime
     *
     * @param project the project name
     * @return -1 if the project doesn't exist, >= 0 in seconds
     */
    int getProjectTime(String project) {
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
                    "SELECT strftime('%s','" + getDateTime(null) + "') - " +
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

    void close(Context context) {
        mDBHelper.close();
        // Release the database lock
        DatabaseLock.release(context, DatabaseLock.DATABASE);
    }

    /**
     * Locks the database (no writes allowed) by beginning an immediate transaction
     * which acquires a RESERVED lock on the database. Call unlock() to end the
     * transaction and discard the lock.
     *
     * @param exclusive set to true to gain an exclusive lock
     */
    void lock(boolean exclusive) {
        if (exclusive) {
            mDatabase.beginTransaction();
        } else {
            mDatabase.beginTransactionNonExclusive();
        }
    }

    /**
     * Unlocks the database by ending a transaction.
     */
    void unlock() {
        mDatabase.endTransaction();
    }

    boolean dumpToCSV(String project, OutputStream os) {
        Context context = mContextRef.get();
        if (context == null) {
            throw new RuntimeException("Unable to export: no Context found.");
        }
        if (exists(project)) {
            // Get the extra data column title or use a the database column name
            Metadata metadata = getProjectMetadata(project);
            if (TextUtils.isEmpty(metadata.extraDataTitle)) {
                metadata.extraDataTitle = KEY_EXTRA_DATA;
            }
            Cursor cursor = mDatabase.rawQuery("SELECT * FROM '" + project + "';", null);
            try {
                os.write((KEY_START_TIME + "," + KEY_END_TIME + "," +
                         context.getString(R.string.delta_seconds) + "," +
                         metadata.extraDataTitle + "\n")
                        .getBytes("utf-8"));
                int startIndex = cursor.getColumnIndex(KEY_START_TIME);
                int endIndex = cursor.getColumnIndex(KEY_END_TIME);
                int extraIndex = cursor.getColumnIndex(KEY_EXTRA_DATA);
                while (cursor.moveToNext()) {
                    String start = cursor.getString(startIndex);
                    String end = cursor.getString(endIndex);
                    String timeInSeconds = "";
                    if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) {
                        Calendar calendar = Calendar.getInstance(Locale.getDefault());
                        calendar.setTime(getDate(start));
                        long startMillis = calendar.getTimeInMillis();
                        calendar.setTime(getDate(end));
                        long endMillis = calendar.getTimeInMillis();
                        timeInSeconds = Long.toString((endMillis - startMillis)/1000);
                    }
                    String output = start + "," + end + "," + timeInSeconds + "," +
                                    cursor.getString(extraIndex) + "\n";
                    os.write(output.getBytes("utf-8"));
                }
            } catch (IOException exception) {
                return false;
            } finally {
                cursor.close();
            }
            return true;
        } else {
            // Project name doesn't exist
            return false;
        }
    }
}