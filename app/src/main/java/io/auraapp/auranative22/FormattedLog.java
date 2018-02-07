package io.auraapp.auranative22;

import android.util.Log;

import static java.lang.String.format;

class FormattedLog {

    static void v(String tag, String msg, Object... args) {
        Log.v(tag, format(msg, args));
    }

    static void d(String tag, String msg, Object... args) {
        Log.d(tag, format(msg, args));
    }

    static void i(String tag, String msg, Object... args) {
        Log.i(tag, format(msg, args));
    }

    static void w(String tag, String msg, Object... args) {
        Log.w(tag, format(msg, args));
    }

    static void e(String tag, String msg, Object... args) {
        Log.e(tag, format(msg, args));
    }


}
