package io.auraapp.auraandroid.common;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.GsonBuilder;

import java.io.Serializable;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfile;

import static io.auraapp.auraandroid.common.IntentFactory.PREFERENCE_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.PREFERENCE_CHANGED_EXTRA_KEY;
import static io.auraapp.auraandroid.common.IntentFactory.PREFERENCE_CHANGED_EXTRA_VALUE;

public class AuraPrefs {

    public static boolean isEnabled(Context context) {
        return get(context)
                .getBoolean(context.getString(R.string.prefs_enabled_key), true);
    }

    public static void putEnabled(Context context, boolean enabled) {
        get(context)
                .edit()
                .putBoolean(context.getString(R.string.prefs_enabled_key), enabled)
                .apply();
    }

    public static String getProfile(Context context) {
        return get(context).getString(context.getString(R.string.prefs_my_profile_key), "{invalidJson}");
    }

    public static void putProfile(Context context, MyProfile profile) {
        get(context)
                .edit()
                .putString(context.getString(R.string.prefs_my_profile_key), new GsonBuilder().create().toJson(profile))
                .apply();
    }

    public static boolean hasAgreedToTerms(Context context) {
        return get(context).getBoolean(context.getString(R.string.prefs_terms_agreed_key), false);
    }

    public static void putHasAgreedToTerms(Context context, boolean agreed) {
        get(context).edit().putBoolean(context.getString(R.string.prefs_terms_agreed_key), agreed).apply();
    }

    public static boolean hasCompletedTutorial(Context context) {
        return get(context).getBoolean(context.getString(R.string.prefs_tutorial_completed_key), false);
    }

    public static void putHasCompletedTutorial(Context context, boolean completed) {
        get(context).edit().putBoolean(context.getString(R.string.prefs_tutorial_completed_key), completed).apply();
    }

    public static boolean shouldHideBrokenBtStackAlert(Context context) {
        return get(context).getBoolean(context.getString(R.string.prefs_hide_broken_bt_warning_key), false);

    }

    public static void putHideBrokenBtStackAlert(Context context) {
        get(context)
                .edit()
                .putBoolean(context.getString(R.string.prefs_hide_broken_bt_warning_key), true)
                .apply();
    }

    public static long getPeerRetention(Context context) {
        return Long.parseLong(get(context).getString(
                context.getString(R.string.prefs_retention_key),
                context.getString(R.string.prefs_retention_default))
        );
    }

    public static void putPeerRetention(long retention, Context context) {
        get(context)
                .edit()
                .putString(context.getString(R.string.prefs_retention_key), "" + retention)
                .apply();
    }

    public static boolean shouldPanicSwipe(Context context) {
        return get(context).getBoolean(context.getString(R.string.prefs_panic_swipe_key), false);
    }

    public static boolean shouldPanicUninstall(Context context) {
        return get(context).getBoolean(context.getString(R.string.prefs_panic_uninstall_key), false);
    }


    public static boolean shouldVibrateOnPeerNotification(Context context) {
        return get(context).getBoolean(context.getString(R.string.prefs_notification_vibrate_key), true);
    }

    public static boolean shouldShowPeerNotification(Context context) {
        return get(context).getBoolean(context.getString(R.string.prefs_notification_show_key), true);
    }

    private static SharedPreferences get(Context context) {
        return context.getSharedPreferences(Config.PREFERENCES_BUCKET, Context.MODE_PRIVATE);
    }

    /**
     * Keeps strong references to all listeners to keep them from being garbage collected
     */
    private static SharedPreferences.OnSharedPreferenceChangeListener listener;

    public static void init(Context context) {
        if (listener != null) {
            return;
        }
        // Listening for changes doesn'T work cross-process so we have to use local broadcasts.
        listener = ($, changedKey) -> {
            Intent intent = new Intent(PREFERENCE_CHANGED_ACTION);
            intent.setPackage(context.getPackageName());
            intent.putExtra(PREFERENCE_CHANGED_EXTRA_KEY, changedKey);
            intent.putExtra(PREFERENCE_CHANGED_EXTRA_VALUE, (Serializable) get(context).getAll().get(changedKey));
            context.sendBroadcast(intent);
        };
        get(context).registerOnSharedPreferenceChangeListener(listener);
    }

    @FunctionalInterface
    public interface OnPrefChanged {
        public void onPrefChanged(Serializable value);
    }

    public static void listen(Context context, int key, OnPrefChanged callback) {

        final String requiredKey = context.getString(key);

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (requiredKey.equals(intent.getExtras().getString(PREFERENCE_CHANGED_EXTRA_KEY))) {
                    callback.onPrefChanged(intent.getExtras().getString(PREFERENCE_CHANGED_EXTRA_VALUE));
                }
            }
        }, IntentFactory.createFilter(PREFERENCE_CHANGED_ACTION));
    }

    @SuppressLint("ApplySharedPref")
    public static void swipe(Context context) {
        get(context).edit().clear().commit();
    }
}
