package com.splitcriteria.timecard;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

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
 * Provides a Service which backs up the user database to a user-specified location.
 * This service is a JobService and can be scheduled as well as started by calling
 * startService()
 */

@TargetApi(24)
public class BackupService extends JobService {

    private static final String TAG = "BackupService";

    private boolean mBackingUp = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Only allow one backup at a time. If startService is called multiple
        // times before another backup is complete, then ignore the backup request
        if (!mBackingUp) {
            mBackingUp = true;
            if (!startBackup(null)) {
                // If unable to start the backup, then stop the service
                mBackingUp = false;
                stopSelf();
            }
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (startBackup(jobParameters)) {
            return true;
        } else {
            jobFinished(jobParameters, true);
            return false;
        }
    }

    /**
     * Starts a backup if the user preferences indicate a backup is desired and if there
     * is a Uri to backup to.
     *
     * @param jobParameters job parameters, if this was called as a scheduled job
     * @return  true, to indicate work is being done on another thread
     */
    private boolean startBackup(final JobParameters jobParameters) {
        // Get the backup Uri
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String uriString = preferences.getString(getString(R.string.preferences_key_backup_uri),
                null);
        // Exit early if there's not backup Uri or if the user has not turned on the backup option
        if (TextUtils.isEmpty(uriString) ||
                !preferences.getBoolean(getString(R.string.preferences_key_backup), false)) {
            return false;
        }
        Uri target = Uri.parse(uriString);
        // Get the database Uri
        File file = getDatabasePath(getString(R.string.default_database_filename));
        Uri source = Uri.fromFile(file);
        if (!DatabaseLock.acquire(this, DatabaseLock.BACKUP)) {
            return false;
        }
        new AsyncTask<Uri, Void, String>() {
            @Override
            protected String doInBackground(Uri... uri) {
                // Get the database source and backup target
                URL source;
                Uri target = uri[0];
                int bytesWritten = 0;
                try {
                    source = new URL(uri[1].toString());
                    // Copy the source to the target
                    InputStream in = new BufferedInputStream(source.openStream());
                    OutputStream out = getContentResolver().openOutputStream(target, "w");
                    if (out == null) {
                        return getString(R.string.error_backup_title,
                                getString(R.string.error_backup_unable_to_open_target));
                    }
                    out = new BufferedOutputStream(out);
                    byte[] buffer = new byte[
                            getResources().getInteger(R.integer.buffer_size_bytes)];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) > 0) {
                        out.write(buffer, 0, bytesRead);
                        bytesWritten += bytesRead;
                    }
                    in.close();
                    out.close();
                } catch (IOException exception) {
                    return getString(R.string.error_backup_title, exception.getMessage());
                }
                // Return the number of bytes written on success
                return Integer.toString(bytesWritten);
            }

            @Override
            protected void onPostExecute(String result) {
                // Test whether the result is an integer
                int bytesWritten;
                try {
                    bytesWritten = Integer.parseInt(result);
                    // If an integer is passed, then let the user know of a successful backup
                    Toast.makeText(BackupService.this,
                                   R.string.toast_backup_success,
                                   Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Successful backup: wrote " + bytesWritten + " bytes");
                } catch (NumberFormatException exception) {
                    // Results which are not numbers are errors
                    Log.e(TAG, result);
                } finally {
                    // Release the database lock
                    DatabaseLock.release(BackupService.this, DatabaseLock.BACKUP);
                    // If this function was not called with job parameters, then it was
                    // started from startService, so we stop ourselves by calling stopSelf
                    // If, on the other hand, this job was created from JobScheduler, then
                    // jobParameters is not null and we call jobFinished to indicate
                    // our work is done.
                    if (jobParameters == null) {
                        // Reset the "backing up" flag
                        mBackingUp = false;
                        stopSelf();
                    } else {
                        jobFinished(jobParameters, false);
                    }
                }
            }

        }.execute(target, source);

        // Work is being done on another thread
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
