package com.crdroid.settings.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.crdroid.settings.preferences.ColorPickerPreference;

public class RecentsSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String MEMBAR_COLOR = "systemui_recents_mem_barcolor";
    private static final String MEM_TEXT_COLOR = "systemui_recents_mem_textcolor";
    private static final String IMMERSIVE_RECENTS = "immersive_recents";
    private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";

    private static final String RECENTS_USE_OMNISWITCH = "recents_use_omniswitch";
    private static final String OMNISWITCH_START_SETTINGS = "omniswitch_start_settings";

    // Package name of the omnniswitch app
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";
    // Intent for launching the omniswitch settings actvity
    public static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
            .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");

    private ColorPickerPreference mMemBarColor;
    private ColorPickerPreference mMemTextColor;
    private SwitchPreference mRecentsClearAll;
    private SwitchPreference mRecentsUseOmniSwitch;
    private Preference mOmniSwitchSettings;
    private boolean mOmniSwitchInitCalled;
    private ListPreference mImmersiveRecents;
    private ListPreference mRecentsClearAllLocation;
    private SharedPreferences mPreferences;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.crdroid_settings_recents);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mContext = getActivity().getApplicationContext();

        mImmersiveRecents = (ListPreference) prefSet.findPreference(IMMERSIVE_RECENTS);
        mImmersiveRecents.setValue(String.valueOf(Settings.System.getInt(
                resolver, Settings.System.IMMERSIVE_RECENTS, 0)));
        mImmersiveRecents.setSummary(mImmersiveRecents.getEntry());
        mImmersiveRecents.setOnPreferenceChangeListener(this);

        mRecentsUseOmniSwitch = (SwitchPreference)
                prefSet.findPreference(RECENTS_USE_OMNISWITCH);

        try {
            mRecentsUseOmniSwitch.setChecked(Settings.System.getInt(resolver,
                    Settings.System.RECENTS_USE_OMNISWITCH) == 1);
            mOmniSwitchInitCalled = true;
        } catch(SettingNotFoundException e){
            // if the settings value is unset
        }
        mRecentsUseOmniSwitch.setOnPreferenceChangeListener(this);

        mOmniSwitchSettings = (Preference)
                prefSet.findPreference(OMNISWITCH_START_SETTINGS);

        // clear all location
        mRecentsClearAllLocation = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3, UserHandle.USER_CURRENT);
        mRecentsClearAllLocation.setValue(String.valueOf(location));
        mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntry());
        mRecentsClearAllLocation.setOnPreferenceChangeListener(this);

        // Recents memory bar bar color
        mMemBarColor  =
                (ColorPickerPreference) findPreference(MEMBAR_COLOR);
        final int intColorBar = Settings.System.getInt(resolver,
                Settings.System.SYSTEMUI_RECENTS_MEM_BARCOLOR, 0x00ffffff);
        String hexColorBar = String.format("#%08x", (0x00ffffff & intColorBar));
        if (hexColorBar.equals("#00ffffff")) {
            mMemBarColor.setSummary(R.string.default_string);
        } else {
            mMemBarColor.setSummary(hexColorBar);
        }
        mMemBarColor.setNewPreviewColor(intColorBar);
        mMemBarColor.setOnPreferenceChangeListener(this);

        // Recents memory bar text color
        mMemTextColor  =
                (ColorPickerPreference) findPreference(MEM_TEXT_COLOR);
        final int intColorText = Settings.System.getInt(resolver,
                Settings.System.SYSTEMUI_RECENTS_MEM_TEXTCOLOR, 0x00ffffff);
        String hexColorText = String.format("#%08x", (0x00ffffff & intColorText));
        if (hexColorText.equals("#00ffffff")) {
            mMemTextColor.setSummary(R.string.default_string);
        } else {
            mMemTextColor.setSummary(hexColorText);
        }
        mMemTextColor.setNewPreviewColor(intColorText);
        mMemTextColor.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mImmersiveRecents) {
            Settings.System.putInt(resolver, Settings.System.IMMERSIVE_RECENTS,
                    Integer.parseInt((String) newValue));
            mImmersiveRecents.setValue((String) newValue);
            mImmersiveRecents.setSummary(mImmersiveRecents.getEntry());

            mPreferences = mContext.getSharedPreferences("recent_settings", Activity.MODE_PRIVATE);
            if (!mPreferences.getBoolean("first_info_shown", false) && newValue != null) {
                getActivity().getSharedPreferences("recent_settings", Activity.MODE_PRIVATE)
                        .edit()
                        .putBoolean("first_info_shown", true)
                        .commit();
                openAOSPFirstTimeWarning();
            }
            return true;
        } else if (preference == mRecentsUseOmniSwitch) {
            boolean value = (Boolean) newValue;

            // if value has never been set before
            if (value && !mOmniSwitchInitCalled){
                openOmniSwitchFirstTimeWarning();
                mOmniSwitchInitCalled = true;
            }

            Settings.System.putInt(
                    resolver, Settings.System.RECENTS_USE_OMNISWITCH, value ? 1 : 0);
            return true;
        } else if (preference == mRecentsClearAllLocation) {
            int location = Integer.parseInt((String) newValue);
            int index = mRecentsClearAllLocation.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, location, UserHandle.USER_CURRENT);
            mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntries()[index]);
            return true;
        } else if (preference == mMemBarColor) {
            String hex = ColorPickerPreference.convertToARGB(
                Integer.parseInt(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.SYSTEMUI_RECENTS_MEM_BARCOLOR,
                    intHex);
            return true;
        } else if (preference == mMemTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                Integer.parseInt(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.SYSTEMUI_RECENTS_MEM_TEXTCOLOR,
                    intHex);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mOmniSwitchSettings){
            startActivity(INTENT_OMNISWITCH_SETTINGS);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void openAOSPFirstTimeWarning() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.aosp_first_time_title))
                .setMessage(getResources().getString(R.string.aosp_first_time_message))
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                }).show();
    }

    private void openOmniSwitchFirstTimeWarning() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.omniswitch_first_time_title))
                .setMessage(getResources().getString(R.string.omniswitch_first_time_message))
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                }).show();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.CRDROID_SETTINGS;
    }
}
