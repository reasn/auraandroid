package io.auraapp.auraandroid.ui.profile.profileModel;

import java.io.Serializable;
import java.util.TreeSet;

import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.SloganComparator;

public class MyProfile implements Serializable {

    String mColor = null;
    float mColorPickerPointX;
    float mColorPickerPointY;
    String mName = null;
    String mText = null;
    final TreeSet<Slogan> mSlogans = new TreeSet<>(new SloganComparator());

    public String getColor() {
        return mColor;
    }

    public String getText() {
        return mText;
    }

    public TreeSet<Slogan> getSlogans() {
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
