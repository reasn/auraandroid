package io.auraapp.auraandroid.common;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Slogan slogan = (Slogan) o;

        return mText != null ? mText.equals(slogan.mText) : slogan.mText == null;
    }

    @Override
    public int hashCode() {
        return mText != null ? mText.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Slogan{mText='" + mText + "'}";
    }
}
