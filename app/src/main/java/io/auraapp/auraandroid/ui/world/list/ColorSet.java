package io.auraapp.auraandroid.ui.world.list;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import io.auraapp.auraandroid.ui.common.ColorHelper;

class ColorSet {
    @ColorInt
    final int mBackground;
    @ColorInt
    final int mText;
    @ColorInt
    final int mAccentBackground;

    private ColorSet(int text, int background, int accentBackground) {
        mText = text;
        mBackground = background;
        mAccentBackground = accentBackground;
    }

    static ColorSet create(@NonNull String color) {
        int background = Color.parseColor(color);
        int accentBackground = ColorHelper.getAccent(background);
        int text;
        if (Math.abs(128 - ColorHelper.getBrightness(background)) > Math.abs(128 - ColorHelper.getBrightness(accentBackground))) {
            // background is further away from average brightness than accentBackground
            text = ColorHelper.getTextColor(background);
        } else {
            text = ColorHelper.getTextColor(accentBackground);
        }

        return new ColorSet(
                text,
                background,
                accentBackground
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColorSet colorSet = (ColorSet) o;

        if (mBackground != colorSet.mBackground) return false;
        if (mText != colorSet.mText) return false;
        return mAccentBackground == colorSet.mAccentBackground;
    }

    @Override
    public int hashCode() {
        int result = mBackground;
        result = 31 * result + mText;
        result = 31 * result + mAccentBackground;
        return result;
    }

    @Override
    public String toString() {
        return "ColorSet{" +
                "mBackground=" + Integer.toHexString(mBackground) +
                ", mText=" + Integer.toHexString(mText) +
                ", mAccentBackground=" + Integer.toHexString(mAccentBackground) +
                '}';
    }
}
