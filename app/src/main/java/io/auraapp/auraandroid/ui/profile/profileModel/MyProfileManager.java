package io.auraapp.auraandroid.ui.profile.profileModel;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.ColorPicker;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_EXTRA_PROFILE;

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
    private final String mProfilePrefKey;
    private final Context mContext;
    private final MyProfile mMyProfile;
    private final Set<MyProfileChangedCallback> mChangedCallbacks = new HashSet<>();

    public MyProfileManager(Context context) {
        mContext = context;

        mProfilePrefKey = context.getString(R.string.prefs_my_profile_key);

        // Without a persisted value, serialization fails and the default profile is persisted.
        @Nullable String serializedProfile = AuraPrefs.getProfile(mContext);

        MyProfile profile;
        try {
            i(TAG, "Deserializing profile %s", serializedProfile);
            profile = new Gson().fromJson(serializedProfile, MyProfile.class);

            i(TAG, "Profile loaded");

        } catch (JsonSyntaxException e) {
            i(TAG, "No valid profile persisted, creating default");
            profile = createDefaultProfile();
            mMyProfile = profile;
            persistProfile(EVENT_NONE, null);
            return;
        }

        if (!(profile instanceof MyProfile)) {
            // In case rubbish has been persisted, observed e.g. "null"
            profile = createDefaultProfile();
            persistProfile(EVENT_NONE, null);
        }
        mMyProfile = profile;
    }

    public void addChangedCallback(MyProfileChangedCallback callback) {
        mChangedCallbacks.add(callback);
    }

    public void removeChangedCallback(MyProfileChangedCallback mProfileChangedCallback) {
        mChangedCallbacks.remove(mProfileChangedCallback);
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
            persistProfile(EVENT_NAME_CHANGED, IntentFactory.LOCAL_MY_PROFILE_NAME_CHANGED_ACTION);
        }
    }

    public void setText(String text) {
        if (!mMyProfile.mText.equals(text)) {
            mMyProfile.mText = text;
            persistProfile(EVENT_TEXT_CHANGED, IntentFactory.LOCAL_MY_PROFILE_TEXT_CHANGED_ACTION);
        }
    }

    public void setColor(ColorPicker.SelectedColor selectedColor) {
        if (!mMyProfile.mColor.equals(selectedColor.getColor())) {
            mMyProfile.mColor = selectedColor.getColor();
            mMyProfile.mColorPickerPointX = selectedColor.getPointX();
            mMyProfile.mColorPickerPointY = selectedColor.getPointY();
            persistProfile(EVENT_COLOR_CHANGED, IntentFactory.LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION);
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
        persistProfile(EVENT_ADOPTED, IntentFactory.LOCAL_MY_PROFILE_ADOPTED_ACTION);
    }

    public void replace(Slogan oldSlogan, Slogan newSlogan) {
        if (mMyProfile.mSlogans.contains(oldSlogan)) {
            mMyProfile.mSlogans.remove(oldSlogan);
        }
        mMyProfile.mSlogans.add(newSlogan);
        persistProfile(EVENT_REPLACED, IntentFactory.LOCAL_MY_PROFILE_REPLACED_ACTION);
    }

    public void dropSlogan(Slogan slogan) {
        mMyProfile.mSlogans.remove(slogan);

        persistProfile(EVENT_DROPPED, IntentFactory.LOCAL_MY_PROFILE_DROPPED_ACTION);
    }

    public void dropAllSlogans() {
        for (Slogan slogan : getProfile().getSlogans().toArray(new Slogan[getProfile().getSlogans().size()])) {
            dropSlogan(slogan);
        }
    }

    private void persistProfile(int event, @Nullable String intentAction) {
        i(TAG, "Persisting my profile, event: %s, profile: %s,", nameEvent(event), mMyProfile.toString());
        AuraPrefs.putProfile(mContext, mMyProfile);

        if (intentAction != null) {
            Intent intent = new Intent(intentAction);
            intent.putExtra(LOCAL_MY_PROFILE_EXTRA_PROFILE, mMyProfile);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }

        if (event != EVENT_NONE) {
            for (MyProfileChangedCallback callback : mChangedCallbacks) {
                callback.myProfileChanged(event);
            }
        }
    }

    public static String nameEvent(int event) {
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

