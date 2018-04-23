package io.auraapp.auraandroid.common;


import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.GsonBuilder;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfile;

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
        return get(context).getBoolean(context.getString(R.string.prefs_terms_agreed), false);
    }

    public static void putHasAgreedToTerms(Context context, boolean agreed) {
        get(context).edit().putBoolean(context.getString(R.string.prefs_terms_agreed), agreed).apply();
    }

    public static boolean hasCompletedTutorial(Context context) {
        return get(context).getBoolean(context.getString(R.string.prefs_tutorial_completed), false);
    }

    public static void putHasCompletedTutorial(Context context, boolean completed) {
        get(context).edit().putBoolean(context.getString(R.string.prefs_terms_agreed), completed).apply();
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
        return Long.parseLong(
                get(context).getString(
                        context.getString(R.string.prefs_retention_key),
                        context.getString(R.string.prefs_retention_default)
                )
        );
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

    public static void listen(Context context, int key, Runnable callback) {
        final String requiredKey = context.getString(key);
        get(context).registerOnSharedPreferenceChangeListener(($, changedKey) -> {
            if (requiredKey.equals(changedKey)) {
                callback.run();
            }
        });
    }

}
