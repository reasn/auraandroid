package io.auraapp.auraandroid;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.main.MainActivity;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class PermissionMissingActivity extends AppCompatActivity {

    private final static int PERMISSION_REQUEST_CODE_ACCESS_FINE_LOCATION = 149;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO style this

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_missing);

        Button button = findViewById(R.id.show_permission_dialog);
        button.setOnClickListener((View $) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE_ACCESS_FINE_LOCATION);
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
