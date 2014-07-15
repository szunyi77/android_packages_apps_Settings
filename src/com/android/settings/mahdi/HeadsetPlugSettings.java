/*
 * Copyright (C) 2014 The Mahdi-Rom Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mahdi;

import java.util.prefs.PreferenceChangeListener;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.mahdi.chameleonos.SeekBarPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.mahdi.preference.AppSelectListPreference;
import com.android.settings.mahdi.SystemSettingCheckBoxPreference;

import java.net.URISyntaxException;

public class HeadsetPlugSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "HeadsetPlugSettings";

    private static final String KEY_HEADSET_PLUG = "headset_plug";
    private static final String KEY_HEADSET_MUSIC_ACTIVE = "headset_plug_music_active";
    private static final String KEY_HEADSET_ACTIONS = "headset_plug_actions";
    private static final String KEY_HEADSET_PLUG_APP_RUNNING = "headset_plug_app_running";
    private static final String KEY_HEADSET_PLUG_FORCE_ACTIONS = "headset_plug_force_actions";

    private AppSelectListPreference mHeadsetPlug;
    private SystemSettingCheckBoxPreference mHeadsetMusicActive;
    private SystemSettingCheckBoxPreference mHeadsetForceAction;
    private ListPreference mHeadsetAction;
    private ListPreference mHeadsetAppRunning;    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.headset_plug_settings);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();

        mHeadsetAction = (ListPreference) findPreference(KEY_HEADSET_ACTIONS);
        mHeadsetAction.setOnPreferenceChangeListener(this);
        mHeadsetForceAction = (SystemSettingCheckBoxPreference) findPreference(KEY_HEADSET_PLUG_FORCE_ACTIONS);
        updateHeadsetActionSummary();

        mHeadsetAppRunning = (ListPreference) findPreference(KEY_HEADSET_PLUG_APP_RUNNING);
        mHeadsetAppRunning.setValue(Integer.toString(Settings.System.getInt(
            getContentResolver(), Settings.System.HEADSET_PLUG_APP_RUNNING, 0)));
        mHeadsetAppRunning.setSummary(mHeadsetAppRunning.getEntry());
        mHeadsetAppRunning.setOnPreferenceChangeListener(this);

        mHeadsetPlug = (AppSelectListPreference) findPreference(KEY_HEADSET_PLUG);
        mHeadsetPlug.setOnPreferenceChangeListener(this);
        mHeadsetMusicActive = (SystemSettingCheckBoxPreference) findPreference(KEY_HEADSET_MUSIC_ACTIVE);
        updateHeadsetPlugSummary();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHeadsetPlug) {
            String value = (String) newValue;
            Settings.System.putString(getContentResolver(),
                    Settings.System.HEADSET_PLUG_ENABLED, value);
            updateHeadsetPlugSummary();
            return true;
        } else if (preference == mHeadsetAction) {
           String value = (String) newValue;
           int val = Integer.parseInt(value);
           Settings.System.putInt(getContentResolver(),
                   Settings.System.HEADSET_PLUG_ACTIONS, val);
           updateHeadsetActionSummary();
           return true;
        } else if (preference == mHeadsetAppRunning) {
           String value = (String) newValue;
           int val = Integer.parseInt(value);
           Settings.System.putInt(getContentResolver(),
                   Settings.System.HEADSET_PLUG_APP_RUNNING, val);
           int index = mHeadsetAppRunning.findIndexOfValue(value);
           mHeadsetAppRunning.setSummary(mHeadsetAppRunning.getEntries()[index]);
           return true;
        }
        return false;
    }

    private void updateHeadsetActionSummary() {
        int value = Settings.System.getInt(
            getContentResolver(), Settings.System.HEADSET_PLUG_ACTIONS, 0);

        mHeadsetAction.setValue(Integer.toString(value));
        mHeadsetAction.setSummary(mHeadsetAction.getEntry());

        if (value == 0) {
            mHeadsetForceAction.setEnabled(false);
        }else {
            mHeadsetForceAction.setEnabled(true);
        }
    }

    private void updateHeadsetPlugSummary() {
        final PackageManager packageManager = getPackageManager();

        mHeadsetPlug.setSummary(getResources().getString(R.string.headset_plug_positive_title));
        mHeadsetMusicActive.setEnabled(false);
        mHeadsetAction.setEnabled(false);
        mHeadsetAppRunning.setEnabled(false);
        mHeadsetForceAction.setEnabled(false);

        String headSetPlugIntentUri = Settings.System.getString(getContentResolver(), Settings.System.HEADSET_PLUG_ENABLED);

        if (headSetPlugIntentUri != null) {
            if(headSetPlugIntentUri.equals(Settings.System.HEADSET_PLUG_SYSTEM_DEFAULT)) {
                 mHeadsetPlug.setSummary(getResources().getString(R.string.headset_plug_neutral_summary));
                 mHeadsetMusicActive.setEnabled(true);
                 mHeadsetAction.setEnabled(true);
                 mHeadsetAppRunning.setEnabled(true);
                 updateHeadsetActionSummary();
            } else {
                Intent headSetPlugIntent = null;
                try {
                    headSetPlugIntent = Intent.parseUri(headSetPlugIntentUri, 0);
                } catch (URISyntaxException e) {
                    headSetPlugIntent = null;
                }

                if (headSetPlugIntent != null) {
                    ResolveInfo info = packageManager.resolveActivity(headSetPlugIntent, 0);
                    if (info != null) {
                        mHeadsetPlug.setSummary(info.loadLabel(packageManager));
                        mHeadsetMusicActive.setEnabled(true);
                        mHeadsetAction.setEnabled(true);
                        mHeadsetAppRunning.setEnabled(true);
                        updateHeadsetActionSummary();
                    }
                }
            }
        }
    }
}
