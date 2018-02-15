package io.auraapp.auraandroid.main;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.SloganComparator;

import static android.content.Context.MODE_PRIVATE;

class MySloganManager {

    private static final int MAX_SLOGANS = 3;

    private final Runnable mNotifyChangeCallback;

    private final TreeSet<Slogan> mMySlogans = new TreeSet<>(new SloganComparator());

    private final Context mContext;

    MySloganManager(Context context, Runnable notifyChangeCallback) {
        mContext = context;
        mNotifyChangeCallback = notifyChangeCallback;
    }

    void init() {

        SharedPreferences prefs = mContext.getSharedPreferences(MainActivity.PREFS_BUCKET, MODE_PRIVATE);
        mMySlogans.clear();
        for (String mySloganText : prefs.getStringSet(MainActivity.PREFS_SLOGANS, new HashSet<>())) {
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
        mNotifyChangeCallback.run();
    }

    void dropSlogan(Slogan slogan) {
        mMySlogans.remove(slogan);

        persistSlogans();
        mNotifyChangeCallback.run();
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

