package io.auraapp.auraandroid.vendor.colorpicker;

import android.content.Context;
import android.content.SharedPreferences;

public class ColorPickerSharedPreferencesManager {

    private SharedPreferences sharedPreferences;

    protected static final String COLOR = "_COLOR";
    protected static final String POSITION_X = "_POSITION_X";
    protected static final String POSITION_Y = "_POSITION_Y";

    protected ColorPickerSharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences("com.skydoves.colorpickerpreference", Context.MODE_PRIVATE);
    }

    protected void putInteger(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    protected int getInteger(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    protected void clearSavedPositions(String positionX, String positionY) {
        sharedPreferences.edit().remove(positionX).apply();
        sharedPreferences.edit().remove(positionY).apply();
    }

    protected void clearSavedData() {
        sharedPreferences.edit().clear().apply();
    }
}
