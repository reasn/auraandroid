package io.auraapp.auraandroid;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.main.MainActivity;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class PermissionMissingActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_LOCATION_REQUEST = 149;
    private static final int REQUEST_CODE_APP_SETTINGS = 144;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO add toolbar with icon

        // TODO style this

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_missing);

        findViewById(R.id.show_permission_dialog).setOnClickListener((View $) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_REQUEST);
            } else {
                throw new RuntimeException("Attempted to show permission dialog for Android < M");
            }
        });
        findViewById(R.id.show_app_settings).setOnClickListener((View $) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, REQUEST_CODE_APP_SETTINGS);

            } else {
                throw new RuntimeException("Attempted to show permission dialog for Android < M");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(this::continuouslyCheckForPermissions);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mHandler == null) {
            mHandler = new Handler();
        }
        continuouslyCheckForPermissions();
    }

    private void continuouslyCheckForPermissions() {
        if (PermissionHelper.granted(this)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(intent);
            return;
        }
        mHandler.postDelayed(this::continuouslyCheckForPermissions, 500);
    }
}
