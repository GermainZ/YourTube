package com.germainz.yourtube;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsActivity extends Activity {
    private static final String PANE_PLAYLIST = "0";
    private static final String PANE_SUBSCRIPTION = "1";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    public static class PrefsFragment extends PreferenceFragment {
        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences);

            ListPreference defaultPanePref = (ListPreference) findPreference("pref_default_pane");
            final EditTextPreference playlistPref = (EditTextPreference) findPreference("pref_playlist");
            final EditTextPreference subscriptionPref = (EditTextPreference) findPreference("pref_subscription");
            Preference showAppIconPref = findPreference("pref_show_app_icon");

            if (defaultPanePref.getValue().equals(PANE_PLAYLIST))
                playlistPref.setEnabled(true);
            else if (defaultPanePref.getValue().equals(PANE_SUBSCRIPTION))
                subscriptionPref.setEnabled(true);

            defaultPanePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (o.equals(PANE_PLAYLIST)) {
                        playlistPref.setEnabled(true);
                        subscriptionPref.setEnabled(false);
                    } else if (o.equals(PANE_SUBSCRIPTION)) {
                        subscriptionPref.setEnabled(true);
                        playlistPref.setEnabled(false);
                    } else {
                        subscriptionPref.setEnabled(false);
                        playlistPref.setEnabled(false);
                    }
                    return true;
                }
            });

            showAppIconPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Activity context = getActivity();
                    int state = (Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                    final ComponentName alias = new ComponentName(context,
                            "com.germainz.yourtube.SettingsActivity-Alias");
                    context.getPackageManager().setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
                    return true;
                }
            });
        }
    }
}
