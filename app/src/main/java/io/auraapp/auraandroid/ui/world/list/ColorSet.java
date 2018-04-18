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
    @ColorInt
    int mAccentText;

    private ColorSet(int background, int text, int accentBackground, int accentText) {
        mBackground = background;
        mText = text;
        mAccentBackground = accentBackground;
        mAccentText = accentText;
    }

    static ColorSet create(@NonNull String color) {
        int background = Color.parseColor(color);
        int accentBackground = ColorHelper.getAccent(background);

        return new ColorSet(
                background,
                ColorHelper.getTextColor(background),
                accentBackground,
                ColorHelper.getTextColor(accentBackground)
        );
    }
}
