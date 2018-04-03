package io.auraapp.auraandroid.ui.profile.profileModel;

import android.content.Context;

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

import static android.content.Context.MODE_PRIVATE;
import static io.auraapp.auraandroid.common.FormattedLog.i;

public class MyProfileManager {

    private static final String TAG = "@aura/ui/common/" + MyProfileManager.class.getSimpleName();
    private final MyProfile mMyProfile;

    public String getColor() {
        return mMyProfile.mColor;
    }

    public MyProfile getProfile() {
        return mMyProfile;
    }


    @FunctionalInterface
    public interface MyProfileChangedCallback {
        public void myProfileChanged(int event);
    }

    private final Set<MyProfileChangedCallback> mChangedCallbacks = new HashSet<>();

    private final Context mContext;

    private static final int EVENT_NONE = 10;
    public final static int EVENT_COLOR_CHANGED = 11;
    public final static int EVENT_NAME_CHANGED = 12;
    public final static int EVENT_TEXT_CHANGED = 13;
    public final static int EVENT_DROPPED = 14;
    public final static int EVENT_ADOPTED = 15;
    public final static int EVENT_REPLACED = 16;

    public MyProfileManager(Context context) {
        mContext = context;

        // Without a persisted value, serialization fails and the default profile is persisted.
        String serializedProfile = mContext
                .getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE)
                .getString(Prefs.PREFS_MY_PROFILE, "}{");

        MyProfile profile;
        try {
            i(TAG, "Profile loaded");
            profile = new Gson().fromJson(serializedProfile, MyProfile.class);
        } catch (JsonSyntaxException e) {
            i(TAG, "No valid profile persisted, creating default");
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
        MyProfile profile = new MyProfile();
        profile.mColor = Config.COMMON_DEFAULT_COLOR;
        profile.mName = "TODO make configurable";
        profile.mText = "TODO make configurable";
        profile.mSlogans.add(Slogan.create(EmojiHelper.replaceShortCode(mContext.getString(R.string.default_slogan))));
        return profile;
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

    public void setColor(String color) {
        if (!mMyProfile.mColor.equals(color)) {
            mMyProfile.mColor = color;
            persistProfile(EVENT_COLOR_CHANGED);
        }
    }

    public boolean spaceAvailable() {
        return mMyProfile.mSlogans.size() < Config.COMMON_SLOGAN_MAX_SLOGANS;
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
}

