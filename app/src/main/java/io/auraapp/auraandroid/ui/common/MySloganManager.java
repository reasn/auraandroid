package io.auraapp.auraandroid.ui.common;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Prefs;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.SloganComparator;

import static android.content.Context.MODE_PRIVATE;

public class MySloganManager {

    @FunctionalInterface
    public interface MySlogansChangedCallback {
        public void onMySlogansChanged(int event);
    }

    private final Set<MySlogansChangedCallback> mNotifyChangeCallbacks = new HashSet<>();

    private final TreeSet<Slogan> mMySlogans = new TreeSet<>(new SloganComparator());

    private final Context mContext;

    public final static int EVENT_DROPPED = 1;
    public final static int EVENT_ADOPTED = 2;
    public final static int EVENT_REPLACED = 3;

    public MySloganManager(Context context) {
        mContext = context;
    }

    public void addChangedCallback(MySlogansChangedCallback callback) {
        mNotifyChangeCallbacks.add(callback);
    }

    public void init() {
        Set<String> defaultSlogans = new HashSet<>();
        defaultSlogans.add(EmojiHelper.replaceShortCode(mContext.getString(R.string.default_slogan)));

        SharedPreferences prefs = mContext.getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE);
        mMySlogans.clear();
        for (String mySloganText : prefs.getStringSet(Prefs.PREFS_SLOGANS, defaultSlogans)) {
            mMySlogans.add(Slogan.create(mySloganText));
        }
    }

    public TreeSet<Slogan> getMySlogans() {
        return mMySlogans;
    }

    public boolean spaceAvailable() {
        return mMySlogans.size() < Config.COMMON_SLOGAN_MAX_SLOGANS;
    }

    public void adopt(Slogan slogan) {
        if (!spaceAvailable()) {
            return;
        }
        if (mMySlogans.contains(slogan)) {
            return;
        }
        mMySlogans.add(slogan);
        persistSlogans();
        for (MySlogansChangedCallback callback : mNotifyChangeCallbacks) {
            callback.onMySlogansChanged(EVENT_ADOPTED);
        }
    }

    public void replace(Slogan oldSlogan, Slogan newSlogan) {
        if (mMySlogans.contains(oldSlogan)) {
            mMySlogans.remove(oldSlogan);
        }
        mMySlogans.add(newSlogan);
        persistSlogans();
        for (MySlogansChangedCallback callback : mNotifyChangeCallbacks) {
            callback.onMySlogansChanged(EVENT_REPLACED);
        }
    }

    public void dropSlogan(Slogan slogan) {
        mMySlogans.remove(slogan);

        persistSlogans();
        for (MySlogansChangedCallback callback : mNotifyChangeCallbacks) {
            callback.onMySlogansChanged(EVENT_DROPPED);
        }
    }

    private void persistSlogans() {

        SharedPreferences.Editor editor = mContext.getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE).edit();
        Set<String> mySloganTexts = new HashSet<>();
        for (Slogan slogan : mMySlogans) {
            mySloganTexts.add(slogan.getText());
        }
        editor.putStringSet(Prefs.PREFS_SLOGANS, mySloganTexts);

        editor.apply();
    }
}

