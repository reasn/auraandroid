package io.auraapp.auraandroid.main;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.SloganComparator;

import static android.content.Context.MODE_PRIVATE;

class MySloganManager {

    @FunctionalInterface
    interface MySlogansChangedCallback {
        void onMySlogansChanged(int event);
    }

    static final int MAX_SLOGANS = 3;

    private final MySlogansChangedCallback mNotifyChangeCallback;

    private final TreeSet<Slogan> mMySlogans = new TreeSet<>(new SloganComparator());

    private final Context mContext;

    final static int EVENT_DROPPED = 1;
    final static int EVENT_ADOPTED = 2;
    final static int EVENT_REPLACED = 3;

    MySloganManager(Context context, MySlogansChangedCallback notifyChangeCallback) {
        mContext = context;
        mNotifyChangeCallback = notifyChangeCallback;
    }

    void init() {

        Set<String> defaultSlogans = new HashSet<>();
        defaultSlogans.add(EmojiHelper.replaceShortCode(mContext.getString(R.string.default_slogan)));

        SharedPreferences prefs = mContext.getSharedPreferences(MainActivity.PREFS_BUCKET, MODE_PRIVATE);
        mMySlogans.clear();
        for (String mySloganText : prefs.getStringSet(MainActivity.PREFS_SLOGANS, defaultSlogans)) {
            mMySlogans.add(Slogan.create(mySloganText));
        }
    }

    TreeSet<Slogan> getMySlogans() {
        return mMySlogans;
    }

    boolean spaceAvailable() {
        return mMySlogans.size() < MAX_SLOGANS;
    }

    void adopt(Slogan slogan) {
        if (!spaceAvailable()) {
            return;
        }
        if (mMySlogans.contains(slogan)) {
            return;
        }
        mMySlogans.add(slogan);
        persistSlogans();
        mNotifyChangeCallback.onMySlogansChanged(EVENT_ADOPTED);
    }

    void replace(Slogan oldSlogan, Slogan newSlogan) {
        if (mMySlogans.contains(oldSlogan)) {
            mMySlogans.remove(oldSlogan);
        }
        mMySlogans.add(newSlogan);
        persistSlogans();
        mNotifyChangeCallback.onMySlogansChanged(EVENT_REPLACED);
    }

    void dropSlogan(Slogan slogan) {
        mMySlogans.remove(slogan);

        persistSlogans();
        mNotifyChangeCallback.onMySlogansChanged(EVENT_DROPPED);
    }

    private void persistSlogans() {

        SharedPreferences.Editor editor = mContext.getSharedPreferences(MainActivity.PREFS_BUCKET, MODE_PRIVATE).edit();
        Set<String> mySloganTexts = new HashSet<>();
        for (Slogan slogan : mMySlogans) {
            mySloganTexts.add(slogan.getText());
        }
        editor.putStringSet(MainActivity.PREFS_SLOGANS, mySloganTexts);

        editor.apply();
    }
}

