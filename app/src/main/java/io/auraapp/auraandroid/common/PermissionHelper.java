package io.auraapp.auraandroid.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class PermissionHelper {

    public static boolean granted(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION);
    }
}
