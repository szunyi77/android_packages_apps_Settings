/*
 * Copyright (C) 2013 Mahdi-Rom
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

import java.io.File;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.view.Window;
import android.widget.Toast;

import com.android.internal.util.mahdi.DeviceUtils;
import com.android.internal.widget.LockPatternUtils;

import com.android.settings.R;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "LockscreenInterface";

    private static final int DLG_ENABLE_EIGHT_TARGETS = 0;

    private static final String LOCKSCREEN_GENERAL_CATEGORY = "lockscreen_general_category";
    private static final String KEY_LOCKSCREEN_MODLOCK_ENABLED = "lockscreen_modlock_enabled";
    private static final String LOCKSCREEN_STYLE_CATEGORY = "lockscreen_style_category";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_BATTERY_STATUS = "lockscreen_battery_status";
    private static final String LOCKSCREEN_SHORTCUTS_CATEGORY = "lockscreen_shortcuts_category";
    private static final String PREF_LOCKSCREEN_EIGHT_TARGETS = "lockscreen_eight_targets";
    private static final String PREF_LOCKSCREEN_TORCH = "lockscreen_glowpad_torch";
    private static final String PREF_LOCKSCREEN_SHORTCUTS = "lockscreen_shortcuts";

    private static final String KEY_SEE_THROUGH = "see_through";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
        
    private CheckBoxPreference mEnableModLock;
    private PreferenceCategory mStyleCategory;
    private Preference mEnableKeyguardWidgets;
    private CheckBoxPreference mSeeThrough;
    private SeekBarPreference mBlurRadius;
    private ListPreference mBatteryStatus;
    private CheckBoxPreference mLockscreenEightTargets;
    private CheckBoxPreference mGlowpadTorch;
    private Preference mShortcuts;

    private boolean mCheckPreferences;

    private Activity mActivity;
    private ContentResolver mResolver;
    private LockPatternUtils mLockPatternUtils;
    private DevicePolicyManager mDPM;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();
        mLockPatternUtils = new LockPatternUtils(getActivity());
        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        createCustomLockscreenView();
    }

    private PreferenceScreen createCustomLockscreenView() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        prefs = getPreferenceScreen();           

        // Find categories
        PreferenceCategory generalCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_GENERAL_CATEGORY);
        PreferenceCategory lockscreen_shortcuts_category = (PreferenceCategory)
                findPreference(LOCKSCREEN_SHORTCUTS_CATEGORY);
        PreferenceCategory lockscreen_style_category = (PreferenceCategory)
                findPreference(LOCKSCREEN_STYLE_CATEGORY);

        mEnableModLock = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MODLOCK_ENABLED);
        if (mEnableModLock != null) {
            mEnableModLock.setOnPreferenceChangeListener(this);
        }

        // Link to widget settings showing summary about the actual status
        // and remove them on low memory devices
        mEnableKeyguardWidgets = prefs.findPreference(KEY_ENABLE_WIDGETS);
        if (mEnableKeyguardWidgets != null) {
            if (ActivityManager.isLowRamDeviceStatic()
                    || mLockPatternUtils.isLockScreenDisabled()) {
                // Widgets take a lot of RAM, so disable them on low-memory devices
                if (mStyleCategory != null) {
                    mStyleCategory.removePreference(prefs.findPreference(KEY_ENABLE_WIDGETS));
                    mEnableKeyguardWidgets = null;
                }
            } else {
                final boolean disabled = (0 != (mDPM.getKeyguardDisabledFeatures(null)
                        & DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL));
                if (disabled) {
                    mEnableKeyguardWidgets.setSummary(
                            R.string.security_enable_widgets_disabled_summary);                
                }
                mEnableKeyguardWidgets.setEnabled(!disabled);
            }
        }

        // lock screen see through
        mSeeThrough = (CheckBoxPreference) findPreference(KEY_SEE_THROUGH);
        if (mSeeThrough != null) {
            mSeeThrough.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, 0) == 1);
        }

        // lock screen blur radius
        mBlurRadius = (SeekBarPreference) findPreference(KEY_BLUR_RADIUS);
        mBlurRadius.setProgress(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
        mBlurRadius.setOnPreferenceChangeListener(this);
        mBlurRadius.setEnabled(mSeeThrough.isChecked() && mSeeThrough.isEnabled());

        mBatteryStatus = (ListPreference) findPreference(KEY_BATTERY_STATUS);
        if (mBatteryStatus != null) {
            mBatteryStatus.setOnPreferenceChangeListener(this);
        }
        
        // Update battery status
        if (mBatteryStatus != null) {
            ContentResolver cr = getActivity().getContentResolver();
            int batteryStatus = Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, 0);
            mBatteryStatus.setValueIndex(batteryStatus);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[batteryStatus]);
        }

        mLockscreenEightTargets = (CheckBoxPreference) findPreference(
                PREF_LOCKSCREEN_EIGHT_TARGETS);
        mLockscreenEightTargets.setChecked(Settings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_EIGHT_TARGETS, 0) == 1);
        mLockscreenEightTargets.setOnPreferenceChangeListener(this);

        mShortcuts = (Preference) findPreference(PREF_LOCKSCREEN_SHORTCUTS);
        mShortcuts.setEnabled(!mLockscreenEightTargets.isChecked());

        // Remove glowpad torch if device doesn't have a torch
        mGlowpadTorch = (CheckBoxPreference) findPreference(PREF_LOCKSCREEN_TORCH);
        if (!Utils.isPhone(getActivity())) {
            lockscreen_shortcuts_category.removePreference(mGlowpadTorch);
        }

        boolean canEnableModLockscreen = false;
        final Bundle keyguard_metadata = Utils.getApplicationMetadata(
                getActivity(), "com.android.keyguard");
        if (keyguard_metadata != null) {
            canEnableModLockscreen = keyguard_metadata.getBoolean(
                    "com.cyanogenmod.keyguard", false);
        }

        if (mEnableModLock != null) {
            prefs.removePreference(mEnableModLock);
            mEnableModLock = null;
        }
                
        final int unsecureUnlockMethod = Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.LOCKSCREEN_UNSECURE_USED, 1);

        //setup custom lockscreen customize view
        if ((unsecureUnlockMethod != 1)
                 || unsecureUnlockMethod == -1) {             
        }
                        
        mCheckPreferences = true;
        return prefs;
    }

    @Override
    public void onResume() {
        super.onResume();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();

        // Update mod lockscreen status
        if (mEnableModLock != null) {
            ContentResolver cr = getActivity().getContentResolver();
            boolean checked = Settings.System.getInt(
                    cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED, 1) == 1;
            mEnableModLock.setChecked(checked);
        }
        if (mEnableKeyguardWidgets != null) {
            if (!lockPatternUtils.getWidgetsEnabled()) {
                mEnableKeyguardWidgets.setSummary(R.string.disabled);
            } else {
                mEnableKeyguardWidgets.setSummary(R.string.enabled);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Checks if the device has hardware buttons.
     * @return has Buttons
     */
    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSeeThrough) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH,
                    mSeeThrough.isChecked() ? 1 : 0);
            mBlurRadius.setEnabled(mSeeThrough.isChecked() && mSeeThrough.isEnabled());
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mEnableModLock) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED,
                    value ? 1 : 0);
            return true;
        } else if (preference == mLockscreenEightTargets) {
            showDialogInner(DLG_ENABLE_EIGHT_TARGETS, (Boolean) objValue);
            return true;
        } else if (preference == mBlurRadius) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS,
                    (Integer) objValue);
            return true;
        }
        return false;
    }

    private void showDialogInner(int id, boolean state) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id, state);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id, boolean state) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putBoolean("state", state);
            frag.setArguments(args);
            return frag;
        }

        LockscreenInterface getOwner() {
            return (LockscreenInterface) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final boolean state = getArguments().getBoolean("state");
            switch (id) {
                case DLG_ENABLE_EIGHT_TARGETS:
                    String message = getOwner().getResources()
                                .getString(R.string.lockscreen_enable_eight_targets_dialog);
                    if (state) {
                        message = message + " " + getOwner().getResources().getString(
                                R.string.lockscreen_enable_eight_targets_enabled_dialog);
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.attention)
                    .setMessage(message)
                    .setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().getContentResolver(),
                                    Settings.System.LOCKSCREEN_EIGHT_TARGETS, state ? 1 : 0);
                            getOwner().mShortcuts.setEnabled(!state);
                            Settings.System.putString(getOwner().getContentResolver(),
                                    Settings.System.LOCKSCREEN_TARGETS, null);
                            for (File pic : getOwner().getActivity().getFilesDir().listFiles()) {
                                if (pic.getName().startsWith("lockscreen_")) {
                                    pic.delete();
                                }
                            }
                            if (state) {
                                Toast.makeText(getOwner().getActivity(),
                                        R.string.lockscreen_target_reset,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            int id = getArguments().getInt("id");
            boolean state = getArguments().getBoolean("state");
            switch (id) {
                case DLG_ENABLE_EIGHT_TARGETS:
                    getOwner().mLockscreenEightTargets.setChecked(!state);
                    break;
             }
        }
    }
}
