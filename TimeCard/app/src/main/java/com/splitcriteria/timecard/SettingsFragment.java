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

import java.util.List;

/**
 * Created by Deuce on 9/29/17.
 */

public class SettingsFragment extends PreferenceFragment implements
        ClickPreference.OnClickListener,
        RestoreService.OnRestoredListener {

    private static final int REQUEST_CODE_CREATE_DOCUMENT = 0;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 1;

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
        mSAFPreference.addOnClickListener(this);
        ((ClickPreference)findPreference(getString(R.string.preferences_key_restore)))
                .addOnClickListener(this);
        ((ClickPreference)findPreference(getString(R.string.preferences_key_restore_alt)))
                .addOnClickListener(this);
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
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().bindService(new Intent(getActivity(), RestoreService.class),
                mRestoreConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
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
    public void onClick(Preference preference) {
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
        } else if (preference.getKey().equals(getString(R.string.preferences_key_restore))) {
            if (mBound) {
                mRestoreService.restore();
            }
        } else if (preference.getKey().equals(getString(R.string.preferences_key_restore_alt))) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-sqlite3");
            intent.putExtra(Intent.EXTRA_TITLE, "*.db");
            startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    @Override
    public void onRestored(boolean success, String errorMsg) {

    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        getPreferenceScreen().getSharedPreferences()
//                .registerOnSharedPreferenceChangeListener(this);
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        getPreferenceScreen().getSharedPreferences()
//                .unregisterOnSharedPreferenceChangeListener(this);
//    }
//
//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//    }
}
