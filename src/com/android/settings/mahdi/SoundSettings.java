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
import android.media.AudioSystem;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.VolumePanel;

import com.android.settings.mahdi.chameleonos.SeekBarPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.mahdi.preference.AppSelectListPreference;
import com.android.settings.mahdi.SystemSettingCheckBoxPreference;

import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class SoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "SoundSettings";

    private static final String CATEGORY_VOLUME = "category_volume";
    private static final String BUTTON_VOLUME_DEFAULT = "button_volume_default_screen";
    private static final String KEY_VOLUME_PANEL_TIMEOUT = "volume_panel_timeout";    
    private static final String KEY_VOLUME_ADJUST_SOUND = "volume_adjust_sounds_enabled";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_SAFE_HEADSET_VOLUME = "safe_headset_volume";
    private static final String KEY_HEADSET_PLUG = "headset_plug";
    private static final String KEY_HEADSET_MUSIC_ACTIVE = "headset_plug_music_active";
    private static final String KEY_HEADSET_ACTIONS = "headset_plug_actions";
    private static final String KEY_HEADSET_PLUG_APP_RUNNING = "headset_plug_app_running";
    private static final String KEY_HEADSET_PLUG_FORCE_ACTIONS = "headset_plug_force_actions";

    private PreferenceCategory volumeCategory;
    private ListPreference mVolumeDefault;
    private SeekBarPreference mVolumePanelTimeout;
    private CheckBoxPreference mVolumeAdjustSound;
    private CheckBoxPreference mSwapVolumeButtons;
    private CheckBoxPreference mSafeHeadsetVolume;
    private AppSelectListPreference mHeadsetPlug;
    private SystemSettingCheckBoxPreference mHeadsetMusicActive;
    private SystemSettingCheckBoxPreference mHeadsetForceAction;
    private ListPreference mHeadsetAction;
    private ListPreference mHeadsetAppRunning;    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.mahdi_sound_settings);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();

        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        mVolumePanelTimeout = (SeekBarPreference) findPreference(KEY_VOLUME_PANEL_TIMEOUT);
        int statusVolumePanelTimeout = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_PANEL_TIMEOUT, 3000);
            mVolumePanelTimeout.setValue(statusVolumePanelTimeout / 1000);
            mVolumePanelTimeout.setOnPreferenceChangeListener(this);

        mVolumeAdjustSound = (CheckBoxPreference) findPreference(KEY_VOLUME_ADJUST_SOUND);

        mSafeHeadsetVolume = (CheckBoxPreference) findPreference(KEY_SAFE_HEADSET_VOLUME);
        mSafeHeadsetVolume.setPersistent(false);
        boolean safeMediaVolumeEnabled = getResources().getBoolean(
                com.android.internal.R.bool.config_safe_media_volume_enabled);
        mSafeHeadsetVolume.setChecked(Settings.System.getInt(resolver,
                Settings.System.SAFE_HEADSET_VOLUME, safeMediaVolumeEnabled ? 1 : 0) != 0);

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

        if (!Utils.isPhone(getActivity())) {
            PreferenceCategory category_volume =
                (PreferenceCategory) findPreference(CATEGORY_VOLUME);
            category_volume.removePreference(mVolumeAdjustSound);       
        }

        if (getResources().getBoolean(com.android.internal.R.bool.config_useFixedVolume)) {
            // device with fixed volume policy, do not display volumes submenu
            getPreferenceScreen().removePreference(findPreference(KEY_VOLUME_PANEL_TIMEOUT));
        }

        if (hasVolumeRocker()) {
            int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = (CheckBoxPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);

            mVolumeDefault = (ListPreference) findPreference(BUTTON_VOLUME_DEFAULT);
            String currentDefault = Settings.System.getString(resolver, Settings.System.VOLUME_KEYS_DEFAULT);
            if (!Utils.isVoiceCapable(getActivity())) {
                removeListEntry(mVolumeDefault, String.valueOf(AudioSystem.STREAM_RING));
            }
            if (currentDefault == null) {
                currentDefault = mVolumeDefault.getEntryValues()[mVolumeDefault.getEntryValues().length - 1].toString();
                mVolumeDefault.setSummary(getString(R.string.button_volume_default_summary));
            }
            mVolumeDefault.setValue(currentDefault);
            mVolumeDefault.setSummary(mVolumeDefault.getEntry());
            mVolumeDefault.setOnPreferenceChangeListener(this);
        } else {
            prefScreen.removePreference(volumeCategory);
        }
    }

    private boolean hasVolumeRocker() {
        return getActivity().getResources().getBoolean(R.bool.config_has_volume_rocker);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeDefault) {
            String value = (String) newValue;
            Settings.System.putString(getActivity().getContentResolver(), Settings.System.VOLUME_KEYS_DEFAULT, value);
            updateVolumeDefault(newValue);
            return true;
        } else if (preference == mVolumePanelTimeout) {
            int volumePanelTimeout = (Integer) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.VOLUME_PANEL_TIMEOUT, volumePanelTimeout * 1000);
            return true;
        } else if (preference == mHeadsetPlug) {
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSafeHeadsetVolume) {
            Settings.System.putInt(getContentResolver(), Settings.System.SAFE_HEADSET_VOLUME,
                    mSafeHeadsetVolume.isChecked() ? 1 : 0);
        } else if (preference == mSwapVolumeButtons) {
            int value = mSwapVolumeButtons.isChecked()
                    ? (Utils.isTablet(getActivity()) ? 2 : 1) : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    private void updateVolumeDefault(Object newValue) {
        int index = mVolumeDefault.findIndexOfValue((String) newValue);
        int value = Integer.valueOf((String) newValue);
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.System.VOLUME_KEYS_DEFAULT, value);
        mVolumeDefault.setSummary(mVolumeDefault.getEntries()[index]);
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

    public void removeListEntry(ListPreference list, String valuetoRemove) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        for (int i = 0; i < list.getEntryValues().length; i++) {
            if (list.getEntryValues()[i].toString().equals(valuetoRemove)) {
                continue;
            } else {
                entries.add(list.getEntries()[i]);
                values.add(list.getEntryValues()[i]);
            }
        }

        list.setEntries(entries.toArray(new CharSequence[entries.size()]));
        list.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }
}
