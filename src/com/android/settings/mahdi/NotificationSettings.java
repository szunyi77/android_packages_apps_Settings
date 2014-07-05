package com.android.settings.mahdi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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

    private static final String PREF_NOTI_REMINDER_SOUND = "noti_reminder_sound";
    private static final String PREF_NOTI_REMINDER_ENABLED = "noti_reminder_enabled";
    private static final String PREF_NOTI_REMINDER_INTERVAL = "noti_reminder_interval";
    private static final String PREF_NOTI_REMINDER_RINGTONE = "noti_reminder_ringtone";
    private static final String KEY_LOCKSCREEN_NOTIFICATONS = "lockscreen_notifications";
    private static final String KEY_NOTIFICATON_PEEK = "notification_peek";
    private static final String KEY_PEEK_PICKUP_TIMEOUT = "peek_pickup_timeout";
    private static final String KEY_PEEK_WAKE_TIMEOUT = "peek_wake_timeout";

    private static final String PEEK_APPLICATION = "com.jedga.peek";

    private Preference mHeadsUp;
    private CheckBoxPreference mReminder;
    private ListPreference mReminderInterval;
    private ListPreference mReminderMode;
    private RingtonePreference mReminderRingtone;
    private PreferenceScreen mLockscreenNotifications;
    private CheckBoxPreference mNotificationPeek;
    private ListPreference mPeekPickupTimeout;
    private ListPreference mPeekWakeTimeout;

    private PackageStatusReceiver mPackageStatusReceiver;
    private IntentFilter mIntentFilter;

    private boolean isPeekAppInstalled() {
        return isPackageInstalled(PEEK_APPLICATION);
    }

    private boolean isPackageInstalled(String packagename) {
        PackageManager pm = getActivity().getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
           return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mPackageStatusReceiver == null) {
            mPackageStatusReceiver = new PackageStatusReceiver();
        }
        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            mIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        }
        getActivity().registerReceiver(mPackageStatusReceiver, mIntentFilter);

        addPreferencesFromResource(R.xml.notification_settings);

        mHeadsUp = findPreference(Settings.System.HEADS_UP_NOTIFICATION);

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

        mLockscreenNotifications = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_NOTIFICATONS);

        mNotificationPeek = (CheckBoxPreference) findPreference(KEY_NOTIFICATON_PEEK);
        mNotificationPeek.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.PEEK_STATE, 0) == 1);
        mNotificationPeek.setOnPreferenceChangeListener(this);
        updateVisiblePreferences();

        mPeekPickupTimeout = (ListPreference) findPreference(KEY_PEEK_PICKUP_TIMEOUT);
        int peekPickupTimeout = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.PEEK_PICKUP_TIMEOUT, 15000, UserHandle.USER_CURRENT);
        mPeekPickupTimeout.setValue(String.valueOf(peekPickupTimeout));
        mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntry());
        mPeekPickupTimeout.setOnPreferenceChangeListener(this);

        mPeekWakeTimeout = (ListPreference) findPreference(KEY_PEEK_WAKE_TIMEOUT);
        int peekWakeTimeout = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.PEEK_WAKE_TIMEOUT, 5000, UserHandle.USER_CURRENT);
        mPeekWakeTimeout.setValue(String.valueOf(peekWakeTimeout));
        mPeekWakeTimeout.setSummary(mPeekWakeTimeout.getEntry());
        mPeekWakeTimeout.setOnPreferenceChangeListener(this);

        UpdateSettings();
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mPackageStatusReceiver, mIntentFilter);
        super.onResume();

        boolean headsUpEnabled = Settings.System.getInt(
                getContentResolver(), Settings.System.HEADS_UP_NOTIFICATION, 0) == 1;
        mHeadsUp.setSummary(headsUpEnabled
                ? R.string.summary_heads_up_enabled : R.string.summary_heads_up_disabled);

        boolean lsnEnabled = Settings.System.getInt(
                getContentResolver(), Settings.System.LOCKSCREEN_NOTIFICATIONS, 0) == 1;
        mLockscreenNotifications.setSummary(lsnEnabled
                ? R.string.summary_lockscreen_notifications_enabled : R.string.summary_lockscreen_notifications_disabled);

        UpdateSettings();
    }

    private void updateState() {
        updatePeekCheckbox();
    }

    private void updatePeekCheckbox() {
        boolean enabled = Settings.System.getInt(getContentResolver(),
                Settings.System.PEEK_STATE, 0) == 1;
        mNotificationPeek.setChecked(enabled && !isPeekAppInstalled());
        mNotificationPeek.setEnabled(!isPeekAppInstalled());
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mPackageStatusReceiver);
        super.onPause();
    }

    public void UpdateSettings() {}

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mReminder) {
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
        } else if (preference == mNotificationPeek) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.PEEK_STATE,
                    value ? 1 : 0);
            updateVisiblePreferences();
            return true;
        } else if (preference == mPeekPickupTimeout) {
            int index = mPeekPickupTimeout.findIndexOfValue((String) objValue);
            int peekPickupTimeout = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                Settings.System.PEEK_PICKUP_TIMEOUT,
                    peekPickupTimeout, UserHandle.USER_CURRENT);
            mPeekPickupTimeout.setSummary(mPeekPickupTimeout.getEntries()[index]);
            return true;
        } else if (preference == mPeekWakeTimeout) {
            int index = mPeekWakeTimeout.findIndexOfValue((String) objValue);
            int peekWakeTimeout = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                Settings.System.PEEK_WAKE_TIMEOUT,
                    peekWakeTimeout, UserHandle.USER_CURRENT);
            mPeekWakeTimeout.setSummary(mPeekWakeTimeout.getEntries()[index]);
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

    private void updateVisiblePreferences() {
        int peek = Settings.System.getInt(getContentResolver(),
                Settings.System.PEEK_STATE, 0);
        int lsn = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS, 0);

        if (peek == 1) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS, 0);
            mLockscreenNotifications.setEnabled(false);
            mLockscreenNotifications.setSummary(R.string.summary_lockscreen_notifications_disabled);
        } else if (lsn == 1) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.PEEK_STATE, 0);
        } else {
            mLockscreenNotifications.setEnabled(true);
            mNotificationPeek.setEnabled(true);
        }
    }

    public class PackageStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                updatePeekCheckbox();
            } else if(action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                updatePeekCheckbox();
            }
        }
    }
}
