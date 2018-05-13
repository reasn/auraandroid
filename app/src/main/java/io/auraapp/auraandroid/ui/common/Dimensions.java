package io.auraapp.auraandroid.ui.common;

import android.content.Context;
import android.util.TypedValue;

public class Dimensions {
    public static int dp2px(int dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
