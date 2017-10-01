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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by Deuce on 9/20/17.
 */

@TargetApi(24)
public class BackupService extends JobService {

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
                    return exception.toString();
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
                    // TODO Log backup success
                    Log.d("Backup", "Successful backup: wrote " + bytesWritten + " bytes");
                } catch (NumberFormatException exception) {
                    // Results which are not numbers are errors
                    // TODO Log backup error somewhere for the user to see
                    Log.e("Backup Error", result);
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
