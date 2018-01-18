package com.samourai.ponydirect;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;
//import android.util.Log;

import com.samourai.ponydirect.R;

public class SettingsActivity extends PreferenceActivity	{

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_root);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final EditTextPreference textPref = (EditTextPreference) findPreference("smsRelay");

        textPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String telno = newValue.toString();
                if (telno != null && telno.length() > 0) {
                    String s = telno.replaceAll("[^\\+0-9]", "");
                    if (s.matches("^\\+[0-9]+$")) {
                        PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.SMS_RELAY, telno);
                    }
                    else {
                        Toast.makeText(SettingsActivity.this, R.string.use_intl_format, Toast.LENGTH_SHORT).show();
                    }
                }

                return true;
            }
        });

        final CheckBoxPreference cbMainNet = (CheckBoxPreference) findPreference("mainNet");
        cbMainNet.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (cbMainNet.isChecked()) {
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.USE_MAINNET, false);
                }
                else {
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.USE_MAINNET, true);
                }

                return true;
            }
        });

        Preference aboutPref = (Preference) findPreference("about");
        aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new AlertDialog.Builder(SettingsActivity.this)
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(R.string.app_name)
                        .setMessage(getText(R.string.app_name) + ", " + getText(R.string.version_name))
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.muletools, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();

                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MuleTools/MuleTools"));
                                startActivity(browserIntent);
                            }
                        }).show();

                return true;
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
