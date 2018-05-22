package io.auraapp.auraandroid.ui.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import io.auraapp.auraandroid.BuildConfig;
import io.auraapp.auraandroid.Communicator.Communicator;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.ui.DialogBuilder;
import io.auraapp.auraandroid.ui.DialogManager;
import io.auraapp.auraandroid.ui.FullWidthDialog;
import io.auraapp.auraandroid.ui.panic.PanicResponderActivity;

import static io.auraapp.auraandroid.ui.panic.PanicResponderActivity.PANIC_TRIGGER_ACTION;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Necessary for splash screen
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        super.onCreate(savedInstanceState);

        AuraPrefs.init(this);

        setContentView(R.layout.settings_activity);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_launcher);
        toolbar.setNavigationOnClickListener($ -> finish());

        SettingsFragment fragment = new SettingsFragment();
        // Fragment doesn't rely on `onAttach` because pre launch testing showed a situation
        // where `onCreate` was fired before `onAttach`
        fragment.mActivity = this;
        getFragmentManager().beginTransaction().replace(R.id.preferences_placeholder, fragment).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        private Handler mHandler = new Handler();
        public DialogManager mDialogManager;
        private DialogManager.DialogState mDialogState;
        public SettingsActivity mActivity;

        private Preference findPref(@StringRes int keyId) {
            return findPreference(mActivity.getString(keyId));
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mDialogState = new DialogManager.DialogState();
            mDialogManager = new DialogManager(mActivity, mDialogState);

            getPreferenceManager().setSharedPreferencesName(Config.PREFERENCES_BUCKET);
            addPreferencesFromResource(R.xml.preferences);


            Runnable update = () -> {
                @StringRes
                int summary;
                if (AuraPrefs.shouldUninstallOnPanic(mActivity)
                        && AuraPrefs.shouldPurgeOnPanic(mActivity)) {
                    summary = R.string.prefs_panic_trigger_summary_purge_uninstall;

                } else if (AuraPrefs.shouldUninstallOnPanic(mActivity)) {
                    summary = R.string.prefs_panic_trigger_summary_uninstall;

                } else if (AuraPrefs.shouldPurgeOnPanic(mActivity)) {
                    summary = R.string.prefs_panic_trigger_summary_purge;

                } else {
                    summary = R.string.prefs_panic_trigger_summary;
                }
                findPref(R.string.prefs_panic_trigger_key).setSummary(summary);
            };

            Preference.OnPreferenceChangeListener listener = (preference, newValue) -> {

                // Get additional confirmation if the user wants to enable panic_purge

                if (preference != null && preference.getKey().equals(mActivity.getString(R.string.prefs_panic_purge_key)) && (Boolean) newValue) {
                    mDialogManager.showConfirm(
                            R.string.prefs_panic_purge_dialog_title,
                            R.string.prefs_panic_purge_dialog_message,
                            R.string.prefs_panic_purge_decline,
                            R.string.prefs_panic_purge_confirm,
                            (enable) -> {
                                if (enable) {
                                    ((CheckBoxPreference) findPref(R.string.prefs_panic_purge_key)).setChecked(true);
                                    update.run();
                                }
                            });
                    return false;
                }

                if (preference != null && preference.getKey().equals(mActivity.getString(R.string.prefs_panic_uninstall_key)) && (Boolean) newValue) {
                    mDialogManager.showConfirm(
                            R.string.prefs_panic_uninstall_dialog_title,
                            R.string.prefs_panic_uninstall_dialog_message,
                            R.string.prefs_panic_uninstall_decline,
                            R.string.prefs_panic_uninstall_confirm,
                            (enable) -> {
                                if (enable) {
                                    ((CheckBoxPreference) findPref(R.string.prefs_panic_uninstall_key)).setChecked(true);
                                    update.run();
                                }
                            });
                    return false;
                }

                // Update summary for panic_trigger
                mHandler.post(update);

                return true;
            };

            findPref(R.string.prefs_panic_uninstall_key).setOnPreferenceChangeListener(listener);
            findPref(R.string.prefs_panic_purge_key).setOnPreferenceChangeListener(listener);

            listener.onPreferenceChange(null, null);

            findPref(R.string.prefs_panic_trigger_key).setOnPreferenceClickListener(preference -> {

                @StringRes int message;
                if (AuraPrefs.shouldUninstallOnPanic(mActivity)
                        && AuraPrefs.shouldPurgeOnPanic(mActivity)) {
                    message = R.string.prefs_panic_trigger_dialog_message_purge_uninstall;

                } else if (AuraPrefs.shouldUninstallOnPanic(mActivity)) {
                    message = R.string.prefs_panic_trigger_dialog_message_uninstall;

                } else if (AuraPrefs.shouldPurgeOnPanic(mActivity)) {
                    message = R.string.prefs_panic_trigger_dialog_message_purge;

                } else {
                    message = -1;
                }

                Runnable trigger = () -> {
                    // Communicator needs its own intent as it deals with panic independently
                    Intent communicatorIntent = new Intent(PANIC_TRIGGER_ACTION);
                    communicatorIntent.setClass(mActivity, Communicator.class);
                    mActivity.startService(communicatorIntent);

                    mActivity.finish();
                    new Handler().post(() -> {
                        Intent intent = new Intent(PANIC_TRIGGER_ACTION);
                        intent.setClass(mActivity, PanicResponderActivity.class);
                        startActivity(intent);
                    });
                };
                if (message == -1) {
                    // If no user data is affected, no confirmation is needed
                    trigger.run();
                } else {
                    mDialogManager.showConfirm(
                            R.string.prefs_panic_trigger_dialog_title,
                            message,
                            R.string.prefs_panic_trigger_decline,
                            R.string.prefs_panic_trigger_confirm,
                            (enable) -> {
                                if (enable) {
                                    trigger.run();
                                }
                            });
                }

                return true;
            });

            try {
                mActivity.getPackageManager().getPackageInfo(mActivity.getString(R.string.prefs_panic_ripple_package_name), 0);
                ((PreferenceCategory) findPref(R.string.prefs_panic_group_key)).removePreference(
                        findPref(R.string.prefs_panic_download_key)
                );
            } catch (PackageManager.NameNotFoundException e) {

                findPref(R.string.prefs_panic_download_key).setOnPreferenceClickListener(preference -> {

                    final String appPackageName = mActivity.getString(R.string.prefs_panic_ripple_package_name);
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (android.content.ActivityNotFoundException exception) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }

                    Intent intentA = new Intent(PANIC_TRIGGER_ACTION);
                    intentA.setClass(mActivity, Communicator.class);
                    mActivity.startService(intentA);
                    return true;
                });
            }

            findPref(R.string.prefs_about_key).setOnPreferenceClickListener(preference -> {

                String message = mActivity.getString(R.string.prefs_about_dialog_message)
                        .replaceAll("VERSION_CODE", BuildConfig.VERSION_CODE + "")
                        .replaceAll("VERSION_NAME", BuildConfig.VERSION_NAME);

                FullWidthDialog dialog = new DialogBuilder(mActivity, mDialogState)
                        .setTitle(R.string.prefs_about_dialog_title)
                        .setMessage(message)
                        .build();
                dialog.getCancelButton().setVisibility(View.GONE);
                dialog.show();
                dialog.getConfirmButton().setText(R.string.prefs_about_dialog_confirm);

                return true;
            });
        }
    }
}
