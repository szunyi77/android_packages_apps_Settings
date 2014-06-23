package com.android.settings.mahdi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.view.Gravity;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NotificationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "NotificationSettings";

    private static final String PREF_HOVER_LONG_FADE_OUT_DELAY = "hover_long_fade_out_delay";
    private static final String PREF_HOVER_EXCLUDE_NON_CLEARABLE = "hover_exclude_non_clearable";
    private static final String PREF_HOVER_EXCLUDE_LOW_PRIORITY = "hover_exclude_low_priority";
    private static final String PREF_HOVER_EXCLUDE_TOPMOST = "hover_exclude_topmost";
    private static final String PREF_HOVER_EXCLUDE_FROM_INSECURE_LOCK_SCREEN = "hover_exclude_from_insecure_lock_screen";
    private static final String PREF_NOTI_REMINDER_SOUND = "noti_reminder_sound";
    private static final String PREF_NOTI_REMINDER_ENABLED = "noti_reminder_enabled";
    private static final String PREF_NOTI_REMINDER_INTERVAL = "noti_reminder_interval";
    private static final String PREF_NOTI_REMINDER_RINGTONE = "noti_reminder_ringtone";

    private Preference mHeadsUp;
    private ListPreference mHoverLongFadeOutDelay;
    private CheckBoxPreference mHoverExcludeNonClearable;
    private CheckBoxPreference mHoverExcludeNonLowPriority;
    private CheckBoxPreference mHoverExcludeTopmost;
    private CheckBoxPreference mHoverExcludeFromInsecureLockScreen;
    private CheckBoxPreference mReminder;
    private ListPreference mReminderInterval;
    private ListPreference mReminderMode;
    private RingtonePreference mReminderRingtone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notification_settings);

        mHeadsUp = findPreference(Settings.System.HEADS_UP_NOTIFICATION);

        mHoverLongFadeOutDelay = (ListPreference) findPreference(PREF_HOVER_LONG_FADE_OUT_DELAY);
        int hoverLongFadeOutDelay = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HOVER_LONG_FADE_OUT_DELAY, 5000, UserHandle.USER_CURRENT);
        mHoverLongFadeOutDelay.setValue(String.valueOf(hoverLongFadeOutDelay));
        mHoverLongFadeOutDelay.setSummary(mHoverLongFadeOutDelay.getEntry());
        mHoverLongFadeOutDelay.setOnPreferenceChangeListener(this);

        mHoverExcludeNonClearable = (CheckBoxPreference) findPreference(PREF_HOVER_EXCLUDE_NON_CLEARABLE);
        mHoverExcludeNonClearable.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HOVER_EXCLUDE_NON_CLEARABLE, 0, UserHandle.USER_CURRENT) == 1);
        mHoverExcludeNonClearable.setOnPreferenceChangeListener(this);

        mHoverExcludeNonLowPriority = (CheckBoxPreference) findPreference(PREF_HOVER_EXCLUDE_LOW_PRIORITY);
        mHoverExcludeNonLowPriority.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HOVER_EXCLUDE_LOW_PRIORITY, 0, UserHandle.USER_CURRENT) == 1);
        mHoverExcludeNonLowPriority.setOnPreferenceChangeListener(this);

        mHoverExcludeTopmost = (CheckBoxPreference) findPreference(PREF_HOVER_EXCLUDE_TOPMOST);
        mHoverExcludeTopmost.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HOVER_EXCLUDE_TOPMOST, 0, UserHandle.USER_CURRENT) == 1);
        mHoverExcludeTopmost.setOnPreferenceChangeListener(this);

        mHoverExcludeFromInsecureLockScreen = (CheckBoxPreference) findPreference(PREF_HOVER_EXCLUDE_FROM_INSECURE_LOCK_SCREEN);
        mHoverExcludeFromInsecureLockScreen.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.HOVER_EXCLUDE_FROM_INSECURE_LOCK_SCREEN, 0, UserHandle.USER_CURRENT) == 1);
        mHoverExcludeFromInsecureLockScreen.setOnPreferenceChangeListener(this);

        mReminder = (CheckBoxPreference) findPreference(PREF_NOTI_REMINDER_ENABLED);
        mReminder.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_ENABLED, 0, UserHandle.USER_CURRENT) == 1);
        mReminder.setOnPreferenceChangeListener(this);

        mReminderInterval = (ListPreference) findPreference(PREF_NOTI_REMINDER_INTERVAL);
        int interval = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_INTERVAL, 0, UserHandle.USER_CURRENT);
        mReminderInterval.setOnPreferenceChangeListener(this);
        updateReminderIntervalSummary(interval);

        mReminderMode = (ListPreference) findPreference(PREF_NOTI_REMINDER_SOUND);
        int mode = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_NOTIFY, 0, UserHandle.USER_CURRENT);
        mReminderMode.setValue(String.valueOf(mode));
        mReminderMode.setOnPreferenceChangeListener(this);
        updateReminderModeSummary(mode);

        mReminderRingtone =
                (RingtonePreference) findPreference(PREF_NOTI_REMINDER_RINGTONE);
        Uri ringtone = null;
        String ringtoneString = Settings.System.getStringForUser(getContentResolver(),
                Settings.System.REMINDER_ALERT_RINGER, UserHandle.USER_CURRENT);
        if (ringtoneString == null) {
            // Value not set, defaults to Default Ringtone
            ringtone = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_RINGTONE);
        } else {
            ringtone = Uri.parse(ringtoneString);
        }
        Ringtone alert = RingtoneManager.getRingtone(getActivity(), ringtone);
        mReminderRingtone.setSummary(alert.getTitle(getActivity()));
        mReminderRingtone.setOnPreferenceChangeListener(this);
        mReminderRingtone.setEnabled(mode != 0);

        UpdateSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean headsUpEnabled = Settings.System.getInt(
                getContentResolver(), Settings.System.HEADS_UP_NOTIFICATION, 0) == 1;
        mHeadsUp.setSummary(headsUpEnabled
                ? R.string.summary_heads_up_enabled : R.string.summary_heads_up_disabled);
        UpdateSettings();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void UpdateSettings() {}

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mHoverLongFadeOutDelay) {
            int index = mHoverLongFadeOutDelay.findIndexOfValue((String) objValue);
            int hoverLongFadeOutDelay = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                Settings.System.HOVER_LONG_FADE_OUT_DELAY,
                    hoverLongFadeOutDelay, UserHandle.USER_CURRENT);
            mHoverLongFadeOutDelay.setSummary(mHoverLongFadeOutDelay.getEntries()[index]);
            return true;
        } else if (preference == mHoverExcludeNonClearable) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HOVER_EXCLUDE_NON_CLEARABLE,
                    (Boolean) objValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHoverExcludeNonLowPriority) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HOVER_EXCLUDE_LOW_PRIORITY,
                    (Boolean) objValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHoverExcludeTopmost) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HOVER_EXCLUDE_TOPMOST,
                    (Boolean) objValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHoverExcludeFromInsecureLockScreen) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.HOVER_EXCLUDE_FROM_INSECURE_LOCK_SCREEN,
                    (Boolean) objValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mReminder) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_ENABLED,
                    (Boolean) objValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mReminderInterval) {
            int interval = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_INTERVAL,
                    interval, UserHandle.USER_CURRENT);
            updateReminderIntervalSummary(interval);
        } else if (preference == mReminderMode) {
            int mode = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_NOTIFY,
                    mode, UserHandle.USER_CURRENT);
            updateReminderModeSummary(mode);
            mReminderRingtone.setEnabled(mode != 0);
            return true;
        } else if (preference == mReminderRingtone) {
            Uri val = Uri.parse((String) objValue);
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), val);
            mReminderRingtone.setSummary(ringtone.getTitle(getActivity()));
            Settings.System.putStringForUser(getContentResolver(),
                    Settings.System.REMINDER_ALERT_RINGER,
                    val.toString(), UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateReminderIntervalSummary(int value) {
        int resId;
        switch (value) {
            case 1000:
                resId = R.string.noti_reminder_interval_1s;
                break;
            case 2000:
                resId = R.string.noti_reminder_interval_2s;
                break;
            case 2500:
                resId = R.string.noti_reminder_interval_2dot5s;
                break;
            case 3000:
                resId = R.string.noti_reminder_interval_3s;
                break;
            case 3500:
                resId = R.string.noti_reminder_interval_3dot5s;
                break;
            case 4000:
                resId = R.string.noti_reminder_interval_4s;
                break;
            default:
                resId = R.string.noti_reminder_interval_1dot5s;
                break;
        }
        mReminderInterval.setValue(Integer.toString(value));
        mReminderInterval.setSummary(getResources().getString(resId));
    }

    private void updateReminderModeSummary(int value) {
        int resId;
        switch (value) {
            case 1:
                resId = R.string.enabled;
                break;
            case 2:
                resId = R.string.noti_reminder_sound_looping;
                break;
            default:
                resId = R.string.disabled;
                break;
        }
        mReminderMode.setSummary(getResources().getString(resId));
    }
}
