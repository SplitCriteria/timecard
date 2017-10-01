package com.splitcriteria.timecard;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
 * A bound Service which Restores a database from a user or client-specified location.
 * IBinder provides interaction methods and the ability to add a OnRestoredListener
 * which will be called when the restoration is complete.
 */

public class RestoreService extends Service {

    private final IBinder mBinder = new RestoreBinder();

    private boolean mRestoreRequested = false;
    private boolean mRestoreCompleted = false;
    private List<WeakReference<OnRestoredListener>> mListeners = new ArrayList<>();

    public class RestoreBinder extends Binder {
        RestoreService getService() {
            return RestoreService.this;
        }
    }

    interface OnRestoredListener {
        // TODO include a client/user option to confirm restore?
        // boolean onPreRestore(differences between databases)
        void onRestored(boolean success, String errorMsg);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Adds a listener (as a WeakReference) which will be called (if it still exists)
     * when the database is restored.
     *
     * @param listener  an instance of OnRestoredListener
     */
    public void addOnRestoredListener(OnRestoredListener listener) {
        boolean listenerExists = false;
        for (WeakReference<OnRestoredListener> listenerRef : mListeners) {
            OnRestoredListener existingListener = listenerRef.get();
            if (existingListener != null && existingListener == listener) {
                listenerExists = true;
                break;
            }
        }
        if (!listenerExists) {
            mListeners.add(new WeakReference<>(listener));
        }
    }

    /**
     * Removes a listener, if it exists
     *
     * @param listener  a OnRestoredListener
     */
    public void removeOnRestoredListener(OnRestoredListener listener) {
        WeakReference matchingRef = null;
        for (WeakReference<OnRestoredListener> listenerRef : mListeners) {
            OnRestoredListener existingListener = listenerRef.get();
            if (existingListener != null && listenerRef.get() == listener) {
                matchingRef = listenerRef;
                break;
            }
        }
        if (matchingRef != null) {
            mListeners.remove(matchingRef);
        }
    }


    /**
     * Starts the restoration process using the backup location
     *
     * @return  true, if a restore has been ordered; false if a restore is currently
     *          being processed or there is another error
     */
    public boolean restore() {
        // Get the backup preferences
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(RestoreService.this);
        String backupUriString = preferences.getString(
                getString(R.string.preferences_key_backup_uri), "");
        // If the backup location doesn't exist, then return an error
        if (TextUtils.isEmpty(backupUriString)) {
            callOnRestoreListeners(false,
                    getString(R.string.error_restore_title,
                            getString(R.string.error_restore_message_backup_uri_empty)));
            return false;
        } else {
            return restore(Uri.parse(backupUriString));
        }
    }

    /**
     * Starts the restoration process
     *
     * @param restoreFromUri Uri to restore from
     * @return  true, if a restore has been ordered; false if a restore is currently
     *          being processed or there is some other error
     */
    public boolean restore(Uri restoreFromUri) {
        // If there hasn't been a request to restore the database yet, or a previously
        // requested restore has completed, then start the restore process
        if (!mRestoreRequested || isRestoreCompleted()) {
            mRestoreRequested = true;
            mRestoreCompleted = false;
            startRestore(restoreFromUri);
            // Return true -- the restore is starting
            return true;
        } else {
            // Return false -- a previously ordered restore is occurring
            return false;
        }
    }

    /**
     * Determines is a previously requested restore was completed.
     *
     * @return true, if a restore was previously requested through restore() and has
     *         completed
     */
    public boolean isRestoreCompleted() {
        return mRestoreRequested && mRestoreCompleted;
    }

    private void callOnRestoreListeners(boolean success, String errorMsg) {
        for (WeakReference<OnRestoredListener> listenerRefs : mListeners) {
            OnRestoredListener listener = listenerRefs.get();
            if (listener != null) {
                listener.onRestored(success, errorMsg);
            }
        }
    }

    private void startRestore(Uri restoreFromUri) {
        // Get the target Uri
        File database = getDatabasePath(getString(R.string.default_database_filename));
        Uri targetUri = Uri.fromFile(database);
        // Acquire a lock on the database
        if (!DatabaseLock.acquire(this, DatabaseLock.RESTORE)) {
            callOnRestoreListeners(false,
                    getString(R.string.error_restore_title,
                            getString(R.string.error_restore_database_locked)));
            return;
        }
        // Get a reference to the database location
        // Create the task which restores the database from a backup
        new AsyncTask<Uri, Void, String>() {

            @Override
            protected String doInBackground(Uri... uris) {
                Uri restoreFromUri = uris[0];
                Uri targetUri = uris[1];
                try {
                    // Open the input and output streams
                    ContentResolver contentResolver = getContentResolver();
                    InputStream in = contentResolver.openInputStream(restoreFromUri);
                    if (in == null) {
                        return getString(R.string.error_restore_title,
                                getString(R.string.error_restore_unable_to_open_source));
                    }
                    in = new BufferedInputStream(in);
                    OutputStream out = contentResolver.openOutputStream(targetUri, "w");
                    if (out == null) {
                        return getString(R.string.error_restore_title,
                                getString(R.string.error_restore_unable_to_open_target));
                    }
                    out = new BufferedOutputStream(out);
                    // Copy the restored file to the target location
                    byte[] buffer = new byte[
                            getResources().getInteger(R.integer.buffer_size_bytes)];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) > 0) {
                        out.write(buffer, 0, bytesRead);
                    }
                    in.close();
                    out.close();
                } catch (FileNotFoundException e) {
                    return getString(R.string.error_restore_title, e.getMessage());
                } catch (IOException e) {
                    return getString(R.string.error_restore_title, e.getMessage());
                }
                // Return an empty String as success
                return "";
            }

            @Override
            protected void onPostExecute(String result) {
                // Release the database lock and set the restore completed flag
                DatabaseLock.release(RestoreService.this, DatabaseLock.RESTORE);
                mRestoreCompleted = true;
                // Inform any listeners that the database is restored
                callOnRestoreListeners(TextUtils.isEmpty(result), result);
            }

        }.execute(restoreFromUri, targetUri);
    }
}