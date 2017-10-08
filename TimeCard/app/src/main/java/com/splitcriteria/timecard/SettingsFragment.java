package com.splitcriteria.timecard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.widget.Toast;

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
 * A PreferenceFragment which allows the user to change application-wide
 * preferences and backup and restore the data in this app
 */

public class SettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener,
        ResultFragment.OnResultListener,
        RestoreService.OnRestoredListener {

    private static final int REQUEST_CODE_CREATE_DOCUMENT = 0;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 1;
    private static final int REQUEST_CODE_RESTORE_BACKUP = 2;
    private static final int REQUEST_CODE_RESTORE_ALTERNATE_BACKUP = 3;

    private static final String TAG_RESTORE_BACKUP = "restore_backup";
    private static final String TAG_RESTORE_ALTERNATE_BACKUP = "restore_alternate_backup";

    private UriPreference mSAFPreference;
    private RestoreService mRestoreService;
    private boolean mBound;

    private ServiceConnection mRestoreConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            RestoreService.RestoreBinder binder = (RestoreService.RestoreBinder)iBinder;
            mRestoreService = binder.getService();
            mRestoreService.addOnRestoredListener(SettingsFragment.this);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        mSAFPreference = (UriPreference)findPreference(
                getString(R.string.preferences_key_backup_uri));
        refreshSAFSummaryText();
        // Set OnClick listeners for the custom preferences
        mSAFPreference.setOnPreferenceClickListener(this);
        findPreference(getString(R.string.preferences_key_restore)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.preferences_key_restore_alt)).setOnPreferenceClickListener(this);
        // Set a listener to the backup switch, which will start
        findPreference(getString(R.string.preferences_key_backup))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                // Start a backup. If the backup preference is "unchecked" the backup
                // will be ignored in BackupService.
                getActivity().startService(new Intent(getActivity(), BackupService.class));
                return true;
            }
        });
        // Bind to the restore service
        getActivity().bindService(new Intent(getActivity(), RestoreService.class),
                mRestoreConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            getActivity().unbindService(mRestoreConnection);
            mBound = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT && resultCode == Activity.RESULT_OK) {
            // Release previous persisted backup Uri permissions
            ContentResolver contentResolver = getActivity().getContentResolver();
            List<UriPermission> uriPermissions = contentResolver.getPersistedUriPermissions();
            for (UriPermission uriPermission : uriPermissions) {
                contentResolver.releasePersistableUriPermission(uriPermission.getUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            // Get the user chosen Uri from the intent
            Uri uri = data.getData();
            // Take the persistent permissions of the Uri
            contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Save the Uri in the preference
            mSAFPreference.setUri(uri);
            // Update the SAF Preference summary
            refreshSAFSummaryText();
            // Now that a backup Uri is chosen, start a backup. If the Backup flag is
            // not set, then the backup will be ignored in BackupService
            getActivity().startService(new Intent(getActivity(), BackupService.class));
        } else if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == Activity.RESULT_OK) {
            // Restore the database from the user chosen database
            if (mBound) {
                mRestoreService.restore(data.getData());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void refreshSAFSummaryText() {
        Uri uri = mSAFPreference.getUri();
        mSAFPreference.setSummary(uri == null ?
                getString(R.string.preferences_description_backup_uri) :
                getString(R.string.preferences_description_backup_uri_set, uri.toString()));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mSAFPreference) {
            // Allow the user to pick a file destination from the storage
            // access framework
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            // Only allow files which can be opened
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // Set the MIME type for SQLite3 database
            intent.setType("application/x-sqlite3");
            // Set a default title
            intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.default_backup_filename));
            // Start the activity to get the file
            startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT);
            return true;
        } else if (preference.getKey().equals(getString(R.string.preferences_key_restore))) {
            // Prompt the user to confirm the restore
            Dialogs.SimpleMessageDialogFragment.createSimpleMessageDialog(
                        getString(R.string.restore_confirm_title),
                        getString(R.string.restore_confirm_message))
                    .setOnResultListener(this)
                    .setRequestCode(REQUEST_CODE_RESTORE_BACKUP)
                    .show(getFragmentManager(), TAG_RESTORE_BACKUP);
            return true;
        } else if (preference.getKey().equals(getString(R.string.preferences_key_restore_alt))) {
            // Prompt the user to confirm the restore
            Dialogs.SimpleMessageDialogFragment.createSimpleMessageDialog(
                        getString(R.string.restore_confirm_title),
                        getString(R.string.restore_confirm_message))
                    .setOnResultListener(this)
                    .setRequestCode(REQUEST_CODE_RESTORE_ALTERNATE_BACKUP)
                    .show(getFragmentManager(), TAG_RESTORE_ALTERNATE_BACKUP);
            return true;
        }
        // Return false is the preference was not handled above
        return false;
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_RESTORE_BACKUP && resultCode == Activity.RESULT_OK) {
            if (mBound) {
                mRestoreService.restore();
            }
        } else if (requestCode == REQUEST_CODE_RESTORE_ALTERNATE_BACKUP &&
                   resultCode == Activity.RESULT_OK) {
            Intent openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            openIntent.addCategory(Intent.CATEGORY_OPENABLE);
            openIntent.setType("*/*");//application/x-sqlite3");
            openIntent.putExtra(Intent.EXTRA_TITLE, "*.db");
            startActivityForResult(openIntent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    @Override
    public void onRestored(boolean success, String errorMsg) {
        String message = success ? getString(R.string.toast_restore_success) : errorMsg;
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
}