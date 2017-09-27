package com.splitcriteria.timecard;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Deuce on 9/20/17.
 */

@TargetApi(24)
public class BackupService extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        // Get the backup Uri
        SharedPreferences preferences = getSharedPreferences(
                getString(R.string.shared_preferences), Context.MODE_PRIVATE);
        String uriString = preferences.getString(getString(R.string.key_backup_uri), null);
        // Exit early if there's not backup Uri
        if (TextUtils.isEmpty(uriString)) {
            return false;
        }
        Uri target = Uri.parse(uriString);
        // Get the database Uri
        File file = getDatabasePath(getString(R.string.database_filename));
        Uri source = Uri.fromFile(file);
        new AsyncTask<Uri, Void, String>() {
            @Override
            protected String doInBackground(Uri... uri) {
                // Acquire a lock on the database
                ProjectData projectData = new ProjectData(getApplicationContext(),
                                                          getString(R.string.database_filename));
                projectData.lock();
                // Get the database source and backup target
                URL source;
                Uri target = uri[0];
                int bytesWritten = 0;
                try {
                    source = new URL(uri[1].toString());
                    // Copy the source to the target
                    InputStream in = new BufferedInputStream(source.openStream());
                    OutputStream out = new BufferedOutputStream(
                            getContentResolver().openOutputStream(target, "w"));
                    byte[] buffer = new byte[1000000];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) > 0) {
                        out.write(buffer, 0, bytesRead);
                        bytesWritten += bytesRead;
                    }
                    in.close();
                    out.close();
                } catch (IOException exception) {
                    return exception.toString();
                } finally {
                    // Unlock the database
                    projectData.unlock();
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
                }
            }

        }.execute(target, source);

        // Return true to indicate processing work on another thread
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
