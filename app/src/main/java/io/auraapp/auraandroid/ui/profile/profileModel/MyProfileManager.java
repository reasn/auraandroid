package io.auraapp.auraandroid.ui.profile.profileModel;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Prefs;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.ColorPicker;

import static android.content.Context.MODE_PRIVATE;
import static io.auraapp.auraandroid.common.FormattedLog.i;

public class MyProfileManager {

    @FunctionalInterface
    public interface MyProfileChangedCallback {
        public void myProfileChanged(int event);
    }

    private static final String TAG = "@aura/ui/common/" + MyProfileManager.class.getSimpleName();
    private static final int EVENT_NONE = 10;
    public final static int EVENT_COLOR_CHANGED = 11;
    public final static int EVENT_NAME_CHANGED = 12;
    public final static int EVENT_TEXT_CHANGED = 13;
    public final static int EVENT_DROPPED = 14;
    public final static int EVENT_ADOPTED = 15;
    public final static int EVENT_REPLACED = 16;

    private final Context mContext;
    private final MyProfile mMyProfile;
    private final Set<MyProfileChangedCallback> mChangedCallbacks = new HashSet<>();

    public MyProfileManager(Context context) {
        mContext = context;

        // Without a persisted value, serialization fails and the default profile is persisted.
        @Nullable String serializedProfile = mContext
                .getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE)
                .getString(Prefs.PREFS_MY_PROFILE, "{invalidJson}");


        MyProfile profile;
        try {
            i(TAG, "Deserializing profile %s", serializedProfile);
            profile = new Gson().fromJson(serializedProfile, MyProfile.class);

            i(TAG, "Profile loaded");
        } catch (JsonSyntaxException e) {
            i(TAG, "No valid profile persisted, creating default");
            profile = createDefaultProfile();
            persistProfile(EVENT_NONE);
        }

        if (!(profile instanceof MyProfile)) {
            // In case rubbish has been persisted, observed e.g. "null"
            profile = createDefaultProfile();
            persistProfile(EVENT_NONE);
        }
        mMyProfile = profile;
    }

    public void addChangedCallback(MyProfileChangedCallback callback) {
        mChangedCallbacks.add(callback);
    }

    public void addAndTriggerChangedCallback(int[] events, MyProfileChangedCallback callback) {
        mChangedCallbacks.add(callback);
        for (int event : events) {
            callback.myProfileChanged(event);
        }
    }

    private MyProfile createDefaultProfile() {
        i(TAG, "Creating default profile");
        MyProfile profile = new MyProfile();
        profile.mColor = Config.PROFILE_DEFAULT_COLOR;
        profile.mColorPickerPointX = Config.PROFILE_DEFAULT_COLOR_X;
        profile.mColorPickerPointY = Config.PROFILE_DEFAULT_COLOR_Y;
        profile.mName = EmojiHelper.replaceShortCode(mContext.getString(R.string.profile_default_name));
        profile.mText = EmojiHelper.replaceShortCode(mContext.getString(R.string.profile_default_text));
        profile.mSlogans.add(Slogan.create(EmojiHelper.replaceShortCode(mContext.getString(R.string.profile_default_slogan))));
        return profile;
    }

    public String getColor() {
        return mMyProfile.mColor;
    }

    public MyProfile getProfile() {
        return mMyProfile;
    }

    public void setName(String name) {
        if (!mMyProfile.mName.equals(name)) {
            mMyProfile.mName = name;
            persistProfile(EVENT_NAME_CHANGED);
        }
    }

    public void setText(String text) {
        if (!mMyProfile.mText.equals(text)) {
            mMyProfile.mText = text;
            persistProfile(EVENT_TEXT_CHANGED);
        }
    }

    public void setColor(ColorPicker.SelectedColor selectedColor) {
        if (!mMyProfile.mColor.equals(selectedColor.getColor())) {
            mMyProfile.mColor = selectedColor.getColor();
            mMyProfile.mColorPickerPointX = selectedColor.getPointX();
            mMyProfile.mColorPickerPointY = selectedColor.getPointY();
            persistProfile(EVENT_COLOR_CHANGED);
        }
    }

    public boolean spaceAvailable() {
        return mMyProfile.mSlogans.size() < Config.PROFILE_SLOGANS_MAX_SLOGANS;
    }

    public void adopt(Slogan slogan) {
        if (!spaceAvailable()) {
            return;
        }
        if (mMyProfile.mSlogans.contains(slogan)) {
            return;
        }
        mMyProfile.mSlogans.add(slogan);
        persistProfile(EVENT_ADOPTED);
    }

    public void replace(Slogan oldSlogan, Slogan newSlogan) {
        if (mMyProfile.mSlogans.contains(oldSlogan)) {
            mMyProfile.mSlogans.remove(oldSlogan);
        }
        mMyProfile.mSlogans.add(newSlogan);
        persistProfile(EVENT_REPLACED);
    }

    public void dropSlogan(Slogan slogan) {
        mMyProfile.mSlogans.remove(slogan);

        persistProfile(EVENT_DROPPED);
    }

    private void persistProfile(int event) {
        i(TAG, "Persisting my profile after " + nameEvent(event));
        mContext.getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE)
                .edit()
                .putString(Prefs.PREFS_MY_PROFILE, new GsonBuilder().create().toJson(mMyProfile))
                .apply();

        if (event != EVENT_NONE) {
            for (MyProfileChangedCallback callback : mChangedCallbacks) {
                callback.myProfileChanged(event);
            }
        }
    }

    private static String nameEvent(int event) {
        switch (event) {
            case EVENT_NONE:
                return "EVENT_NONE";
            case EVENT_COLOR_CHANGED:
                return "EVENT_COLOR_CHANGE";
            case EVENT_NAME_CHANGED:
                return "EVENT_NAME_CHANGE";
            case EVENT_TEXT_CHANGED:
                return "EVENT_TEXT_CHANGE";
            case EVENT_DROPPED:
                return "EVENT_DROPPED";
            case EVENT_ADOPTED:
                return "EVENT_ADOPTED";
            case EVENT_REPLACED:
                return "EVENT_REPLACED";
            default:
                throw new RuntimeException("Unknown event " + event);
        }
    }
}

