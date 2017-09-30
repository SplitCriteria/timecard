package com.splitcriteria.timecard;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by Deuce on 9/29/17.
 */

public class SettingsFragment extends PreferenceFragment {

    private static final int REQUEST_CODE_CREATE_DOCUMENT = 0;

    private SAFPreference mSAFPreference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        mSAFPreference = (SAFPreference)findPreference(
                getString(R.string.preferences_key_backup_uri));
        refreshSAFSummaryText();
        mSAFPreference.addOnClickListener(new SAFPreference.OnClickListener() {
            @Override
            public void onClick() {
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
            }
        });
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
