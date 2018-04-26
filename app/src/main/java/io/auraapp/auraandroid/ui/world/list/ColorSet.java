package io.auraapp.auraandroid.ui.world.list;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import io.auraapp.auraandroid.ui.common.ColorHelper;

class ColorSet {
    @ColorInt
    int mBackground;
    @ColorInt
    int mText;
    @ColorInt
    int mAccentBackground;

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
}
