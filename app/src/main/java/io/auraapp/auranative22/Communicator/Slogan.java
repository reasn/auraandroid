package io.auraapp.auranative22.Communicator;

import android.support.annotation.NonNull;

import java.io.Serializable;

public class Slogan implements Serializable, Comparable {
    private String mText;

    public static Slogan create(@NonNull String text) {
        Slogan slogan = new Slogan();
        slogan.mText = text;
        return slogan;
    }

    public String getText() {
        return mText;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        if (!(o instanceof Slogan)) {
            throw new RuntimeException("Can only compare to " + Slogan.class);
        }
        if (mText == null) {
            throw new RuntimeException("mText must not be null");
        }
        return mText.compareTo(((Slogan) o).mText);
    }
}
