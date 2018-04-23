package io.auraapp.auraandroid.ui.settings;


import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import io.auraapp.auraandroid.BuildConfig;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.ui.DialogBuilder;
import io.auraapp.auraandroid.ui.DialogManager;
import io.auraapp.auraandroid.ui.FullWidthDialog;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Necessary for splash screen
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.icon_circle);
        toolbar.setNavigationOnClickListener($ -> finish());

        getFragmentManager().beginTransaction().replace(R.id.preferences_placeholder, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        public Context mContext;
        public DialogManager mDialogManager;
        private DialogManager.DialogState mDialogState;

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
