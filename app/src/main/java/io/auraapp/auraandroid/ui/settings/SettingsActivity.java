package io.auraapp.auraandroid.ui.settings;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
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
        fragment.mActivity = this;
        getFragmentManager().beginTransaction().replace(R.id.preferences_placeholder, fragment).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        public Context mContext;
        private Handler mHandler = new Handler();
        public DialogManager mDialogManager;
        private DialogManager.DialogState mDialogState;
        public SettingsActivity mActivity;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mContext = context;
            mDialogState = new DialogManager.DialogState();
            mDialogManager = new DialogManager(context, mDialogState);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(Config.PREFERENCES_BUCKET);
            addPreferencesFromResource(R.xml.preferences);


            Preference.OnPreferenceChangeListener listener = ($, $$) -> {
                mHandler.post(() -> {
                    @StringRes
                    int summary;
                    if (AuraPrefs.shouldPanicUninstall(mContext)
                            && AuraPrefs.shouldPanicSwipe(mContext)) {
                        summary = R.string.prefs_panic_trigger_summary_swipe_uninstall;

                    } else if (AuraPrefs.shouldPanicUninstall(mContext)) {
                        summary = R.string.prefs_panic_trigger_summary_uninstall;

                    } else if (AuraPrefs.shouldPanicSwipe(mContext)) {
                        summary = R.string.prefs_panic_trigger_summary_swipe;

                    } else {
                        summary = R.string.prefs_panic_trigger_summary;
                    }
                    findPreference(mContext.getString(R.string.prefs_panic_trigger_key)).setSummary(summary);
                });
                return true;
            };

            findPreference(mContext.getString(R.string.prefs_panic_uninstall_key)).setOnPreferenceChangeListener(listener);
            findPreference(mContext.getString(R.string.prefs_panic_swipe_key)).setOnPreferenceChangeListener(listener);

            listener.onPreferenceChange(null, null);

            findPreference(mContext.getString(R.string.prefs_panic_trigger_key)).setOnPreferenceClickListener(preference -> {

                Intent intentA = new Intent(PANIC_TRIGGER_ACTION);
                intentA.setClass(mContext, Communicator.class);
                mContext.startService(intentA);

                mActivity.finish();
                new Handler().post(() -> {
                    Intent intentD = new Intent(PANIC_TRIGGER_ACTION);
                    intentD.setClass(mContext, PanicResponderActivity.class);
                    startActivity(intentD);
                });

                return true;
            });

            findPreference(mContext.getString(R.string.prefs_panic_download_key)).setOnPreferenceClickListener(preference -> {

                final String appPackageName = mContext.getString(R.string.prefs_panic_ripple_package_name);
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException exception) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }

                Intent intentA = new Intent(PANIC_TRIGGER_ACTION);
                intentA.setClass(mContext, Communicator.class);
                mContext.startService(intentA);
                return true;
            });

            findPreference(mContext.getString(R.string.prefs_about_key)).setOnPreferenceClickListener(preference -> {

                String message = mContext.getString(R.string.prefs_about_dialog_message)
                        .replaceAll("VERSION_CODE", BuildConfig.VERSION_CODE + "")
                        .replaceAll("VERSION_NAME", BuildConfig.VERSION_NAME);

                FullWidthDialog dialog = new DialogBuilder(mContext, mDialogState)
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
