package io.auraapp.auraandroid.common;

import android.util.Log;

import static java.lang.String.format;

public class FormattedLog {

    public static void v(String tag, String msg, Object... args) {
        joinIterablesInPlace(args);
        Log.v(tag, format(msg, args));
    }

    public static void d(String tag, String msg, Object... args) {
        joinIterablesInPlace(args);
        Log.d(tag, format(msg, args));
    }

    public static void i(String tag, String msg, Object... args) {
        joinIterablesInPlace(args);
        Log.i(tag, format(msg, args));
    }

    public static void w(String tag, String msg, Object... args) {
        joinIterablesInPlace(args);
        Log.w(tag, format(msg, args));
    }

    public static void e(String tag, String msg, Object... args) {
        joinIterablesInPlace(args);
        Log.e(tag, format(msg, args));
    }

    private static void joinIterablesInPlace(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Iterable) {
                StringBuilder concat = new StringBuilder();
                for (Object element : (Iterable) args[i]) {
                    if (concat.length() > 0) {
                        concat.append(", ");
                    }
                    concat.append(element);
                }
                args[i] = concat.toString();
            }
        }
    }
}
