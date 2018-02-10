package io.auraapp.auranative22;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auranative22.Communicator.Slogan;

import static android.content.Context.MODE_PRIVATE;

class MySloganManager {

    private final Runnable mNotifyChangeCallback;

    private static final String PREFS_BUCKET = "prefs";
    private static final String PREFS_SLOGANS = "slogans";

    private final TreeSet<Slogan> mMySlogans = new TreeSet<>(new SloganComparator());

    private final Context mContext;

    MySloganManager(Context context, Runnable notifyChangeCallback) {
        mContext = context;
        mNotifyChangeCallback = notifyChangeCallback;
    }

    void init() {

        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_BUCKET, MODE_PRIVATE);
        mMySlogans.clear();
        for (String mySloganText : prefs.getStringSet(PREFS_SLOGANS, new HashSet<>())) {
            mMySlogans.add(Slogan.create(mySloganText));
        }
// move invocation to activity
        mNotifyChangeCallback.run();
    }

    TreeSet<Slogan> getMySlogans() {
        return mMySlogans;
    }

    boolean spaceAvailable() {
        return mMySlogans.size() < 3;
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

        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_BUCKET, MODE_PRIVATE).edit();
        Set<String> mySloganTexts = new HashSet<>();
        for (Slogan slogan : mMySlogans) {
            mySloganTexts.add(slogan.getText());
        }
        editor.putStringSet(PREFS_SLOGANS, mySloganTexts);

        editor.apply();
    }
}

