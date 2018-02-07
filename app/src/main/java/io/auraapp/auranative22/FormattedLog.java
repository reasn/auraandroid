package io.auraapp.auranative22;

import android.util.Log;

import static java.lang.String.format;

public class FormattedLog {

    public static void v(String tag, String msg, Object... args) {
        Log.v(tag, format(msg, args));
    }

    public static void d(String tag, String msg, Object... args) {
        Log.d(tag, format(msg, args));
    }

    public static void i(String tag, String msg, Object... args) {
        Log.i(tag, format(msg, args));
    }

    public static void w(String tag, String msg, Object... args) {
        Log.w(tag, format(msg, args));
    }

    public static void e(String tag, String msg, Object... args) {
        Log.e(tag, format(msg, args));
    }


}
