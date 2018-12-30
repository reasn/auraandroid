package io.auraapp.auraandroid.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.common.Slogan;

public class MyProfile implements Serializable {

    public String mColor = null;
    public float mColorPickerPointX;
    public float mColorPickerPointY;
    public String mName = null;
    public String mText = null;
    public final List<Slogan> mSlogans = new ArrayList<>();

    public String getColor() {
        return mColor;
    }

    public String getText() {
        return mText;
    }

    public List<Slogan> getSlogans() {
        return mSlogans;
    }

    public String getName() {
        return mName;
    }

    public float getColorPickerPointX() {
        return mColorPickerPointX;
    }

    public float getColorPickerPointY() {
        return mColorPickerPointY;
    }

    @Override
    public String toString() {
        return "MyProfile{" +
                "mColor='" + mColor + '\'' +
                ", mName='" + mName + '\'' +
                ", mText='" + mText + '\'' +
                ", mColorPickerPointX=" + mColorPickerPointX +
                ", mColorPickerPointY=" + mColorPickerPointY +
                ", mSlogans=" + mSlogans +
                '}';
    }
}
