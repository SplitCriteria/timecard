package com.splitcriteria.timecard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.Arrays;
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

public class ProjectSettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_PROJECT_NAME = "project_name";

    private String mProjectName;

    static ProjectSettingsFragment createProjectSettingsFragment(String projectName) {
        ProjectSettingsFragment fragment = new ProjectSettingsFragment();
        Bundle args = new Bundle();
        args.putString(KEY_PROJECT_NAME, projectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.project_preferences);
        // Get the project name from the arguments
        Bundle args = getArguments();
        mProjectName = args.getString(KEY_PROJECT_NAME);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPreferences();
    }

    private void refreshPreferences() {
        // Get the project's metadata
        ProjectData projectData = new ProjectData(getActivity());
        ProjectData.Metadata metadata = projectData.getProjectMetadata(mProjectName);
        projectData.close(getActivity());
        // Because each of these preferences have custom data backing (i.e. not in shared
        // preferences) we'll need to catch the value change and enter the information in the
        // database ourselves
        // TODO Once API 26 is targeted or a PreferenceCompat comes out with support, use
        // the method setPreferenceDataStore()
        Preference preference =  findPreference(
                getString(R.string.preferences_project_instant_key));
        preference.setOnPreferenceChangeListener(this);
        ((SwitchPreference)preference).setChecked(metadata.noDuration);
        preference = findPreference(getString(R.string.preferences_project_location_key));
        preference.setOnPreferenceChangeListener(this);
        ((SwitchPreference)preference).setChecked(metadata.trackLocation);
        preference = findPreference(
                getString(R.string.preferences_project_suppress_notification_key));
        preference.setOnPreferenceChangeListener(this);
        ((SwitchPreference)preference).setChecked(metadata.suppressNotification);
        preference = findPreference(getString(R.string.preferences_project_extra_data_key));
        preference.setOnPreferenceChangeListener(this);
        ((SwitchPreference)preference).setChecked(metadata.usesExtraData);
        preference = findPreference(getString(R.string.preferences_project_extra_data_header_key));
        preference.setOnPreferenceChangeListener(this);
        preference.setSummary(TextUtils.isEmpty(metadata.extraDataTitle) ?
                getString(R.string.preferences_project_extra_data_header_summary_default) :
                getString(R.string.preferences_project_extra_data_header_summary,
                        metadata.extraDataTitle));
        preference = findPreference(getString(R.string.preferences_project_default_extra_key));
        preference.setOnPreferenceChangeListener(this);
        preference.setSummary(TextUtils.isEmpty(metadata.defaultExtraData) ?
                getString(R.string.preferences_project_default_extra_description) :
                getString(R.string.preferences_project_default_extra_set_description,
                        metadata.defaultExtraData));
        // Set the text to simulate the extra data for this project. The ProjectData
        // database holds all the extra data for each project, but the project_preferences.xml
        // file  directs the EditTextPreference only to save the data to a single key
        // (i.e. no project specific preference data saved).
        ((EditTextPreference)preference).setText(metadata.defaultExtraData);
        preference = findPreference(getString(R.string.preferences_project_summary_key));
        preference.setOnPreferenceChangeListener(this);
        String[] summaryMethods = getResources().getStringArray(R.array.preferences_summary_types);
        List<String> summaryValues = Arrays.asList(getResources().getStringArray(
                R.array.preferences_summary_type_values));
        int valueIndex = summaryValues.indexOf(metadata.dataSummaryMethod);
        preference.setSummary(getString(R.string.preferences_project_summary_description,
                summaryMethods[valueIndex]));
        ((ListPreference)preference).setValue(metadata.dataSummaryMethod);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        // Get the project data and its metadata
        ProjectData projectData = new ProjectData(getActivity());
        ProjectData.Metadata metadata = projectData.getProjectMetadata(mProjectName);
        // Get the updated project settings
        if (key.equals(getString(R.string.preferences_project_instant_key))) {
            metadata.noDuration = (Boolean)newValue;
        } else if (key.equals(getString(R.string.preferences_project_location_key))) {
            metadata.trackLocation = (Boolean)newValue;
        } else if (key.equals(getString(R.string.preferences_project_extra_data_key))) {
            metadata.usesExtraData = (Boolean)newValue;
        } else if (key.equals(getString(R.string.preferences_project_default_extra_key))) {
            metadata.defaultExtraData = (String)newValue;
            // Update the summary
            preference.setSummary(TextUtils.isEmpty(metadata.defaultExtraData) ?
                    getString(R.string.preferences_project_default_extra_description) :
                    getString(R.string.preferences_project_default_extra_set_description,
                            metadata.defaultExtraData));
        } else if (key.equals(getString(R.string.preferences_project_summary_key))) {
            metadata.dataSummaryMethod = (String)newValue;
            String[] summaryMethods = getResources().getStringArray(
                    R.array.preferences_summary_types);
            List<String> summaryValues = Arrays.asList(getResources().getStringArray(
                    R.array.preferences_summary_type_values));
            int valueIndex = summaryValues.indexOf(metadata.dataSummaryMethod);
            preference.setSummary(getString(R.string.preferences_project_summary_description,
                    summaryMethods[valueIndex]));
        } else if (key.equals(getString(R.string.preferences_project_extra_data_header_key))) {
            metadata.extraDataTitle = (String)newValue;
            preference.setSummary(TextUtils.isEmpty(metadata.extraDataTitle) ?
                    getString(R.string.preferences_project_extra_data_header_summary_default) :
                    getString(R.string.preferences_project_extra_data_header_summary,
                            metadata.extraDataTitle));
        } else if (key.equals(getString(R.string.preferences_project_suppress_notification_key))) {
            metadata.suppressNotification = (Boolean)newValue;
        }
        // Update the project's metadata
        projectData.updateMetadata(mProjectName, metadata);
        projectData.close(getActivity());
        // Return true to let the Preference update its state
        return true;
    }
}