/*
 * Copyright (C) 2013 The Mahdi Rom project
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

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.IWindowManager;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ButtonSettings";

    private static final String KEY_NAVIGATION_BAR_CATEGORY = "navigation_bar_category";
    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_NAV_BUTTONS_EDIT = "nav_buttons_edit";
    private static final String KEY_NAV_BUTTONS_HEIGHT = "nav_buttons_height";
    private static final String KEY_NAVIGATION_CONTROL = "navigation_control";
    private static final String KEY_NAVIGATION_RING = "navigation_ring";
    private static final String KEY_HARDWARE_KEYS_CATEGORY = "hardware_keys_category";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";

    private PreferenceScreen mNavButtonEdit;
    private ListPreference mNavButtonsHeight;
    private PreferenceScreen mNavigationControl;
    private PreferenceScreen mNavigationRing;    
    private PreferenceScreen mHardwareKeys;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	Resources res = getResources();	

        addPreferencesFromResource(R.xml.button_settings);

	PreferenceScreen prefSet = getPreferenceScreen();

	mNavButtonEdit = (PreferenceScreen) findPreference(KEY_NAV_BUTTONS_EDIT);

        mNavButtonsHeight = (ListPreference) findPreference(KEY_NAV_BUTTONS_HEIGHT);
        mNavButtonsHeight.setOnPreferenceChangeListener(this);
        int statusNavButtonsHeight = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                 Settings.System.NAV_BUTTONS_HEIGHT, 48);
        mNavButtonsHeight.setValue(String.valueOf(statusNavButtonsHeight));
        mNavButtonsHeight.setSummary(mNavButtonsHeight.getEntry());

	mNavigationControl = (PreferenceScreen) findPreference(KEY_NAVIGATION_CONTROL); 

	mNavigationRing = (PreferenceScreen) findPreference(KEY_NAVIGATION_RING);                
      
	// Only show the hardware keys config on a device that does not have a navbar
        mHardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        if (mHardwareKeys != null) {
            if (!res.getBoolean(R.bool.config_has_hardware_buttons)) {
                getPreferenceScreen().removePreference(mHardwareKeys);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(KEY_HARDWARE_KEYS_CATEGORY));
            }        
        }
    }

        public boolean onPreferenceChange(Preference preference, Object objValue) {
            if (preference == mNavButtonsHeight) {
                int statusNavButtonsHeight = Integer.valueOf((String) objValue);
                int index = mNavButtonsHeight.findIndexOfValue((String) objValue);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.NAV_BUTTONS_HEIGHT, statusNavButtonsHeight);
                mNavButtonsHeight.setSummary(mNavButtonsHeight.getEntries()[index]);
                return true;
            }
            return false;
        }
    }    
