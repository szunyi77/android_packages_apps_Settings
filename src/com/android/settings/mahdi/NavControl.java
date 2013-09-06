/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class NavControl extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String NAVIGATION_BUTTON_COLOR = "navigation_button_color";
    private static final String NAVIGATION_BUTTON_GLOW_COLOR = "navigation_button_glow_color";
    private static final String NAVIGATION_BUTTON_GLOW_TIME =
            "navigation_button_glow_time";

    private static final String KEY_NAVIGATION_BAR = "navigation_bar";

    private Preference mNavigationButtonColor;
    private Preference mNavigationButtonGlowColor;
    private SeekBarPreference mNavigationButtonGlowTime;

    private ContentResolver mContentResolver;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.nav_control);

        PreferenceScreen prefSet = getPreferenceScreen();

        mContext = getActivity().getApplicationContext();
        mContentResolver = mContext.getContentResolver();

        mNavigationButtonColor =
                (Preference) prefSet.findPreference(NAVIGATION_BUTTON_COLOR);

        mNavigationButtonGlowColor =
                (Preference) prefSet.findPreference(NAVIGATION_BUTTON_GLOW_COLOR);

        mNavigationButtonGlowTime =
                (SeekBarPreference) prefSet.findPreference(NAVIGATION_BUTTON_GLOW_TIME);
        mNavigationButtonGlowTime.setDefault(Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(), Settings.System.NAVIGATION_BUTTON_GLOW_TIME, 500));
        mNavigationButtonGlowTime.setOnPreferenceChangeListener(this);

    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mNavigationButtonColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mButtonColorListener, Settings.System.getInt(mContentResolver,
                    Settings.System.NAVIGATION_BUTTON_COLOR,
                    getActivity().getApplicationContext().getResources().getColor(
                    com.android.internal.R.color.transparent)));
            cp.setDefaultColor(0x00000000);
            cp.show();
            return true;
        } else if (preference == mNavigationButtonGlowColor) {
            ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                    mGlowColorListener, Settings.System.getInt(mContentResolver,
                    Settings.System.NAVIGATION_BUTTON_GLOW_COLOR,
                    getActivity().getApplicationContext().getResources().getColor(
                    com.android.internal.R.color.transparent)));
            cp.setDefaultColor(0x00000000);
            cp.show();
            return true;
        }
          return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNavigationButtonGlowTime) {
            int value = (Integer) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BUTTON_GLOW_TIME, value);
        }
        return true;
    }


    ColorPickerDialog.OnColorChangedListener mButtonColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NAVIGATION_BUTTON_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };

    ColorPickerDialog.OnColorChangedListener mGlowColorListener =
        new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NAVIGATION_BUTTON_GLOW_COLOR, color);
            }
            public void colorUpdate(int color) {
            }
    };
}
