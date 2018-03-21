package io.auraapp.auraandroid.tutorial;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ScreenPager;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.main.InfoBox;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class PermissionMissingFragment extends Fragment {

    private final static int REQUEST_CODE_LOCATION_REQUEST = 149;
    private static final int REQUEST_CODE_APP_SETTINGS = 144;
    private Handler mHandler;
    private Context mContext;
    private ScreenPager mPager;
    private ViewGroup mView;

    public static PermissionMissingFragment create(Context context, ScreenPager pager) {
        PermissionMissingFragment fragment = new PermissionMissingFragment();
        fragment.mContext = context;
        fragment.mPager = pager;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = (ViewGroup) inflater.inflate(R.layout.fragment_screen_permission_missing, container, false);

        InfoBox infoBox = mView.findViewById(R.id.info_box);
        infoBox.setButtonClickListener((View $) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_REQUEST);
            } else {
                throw new RuntimeException("Attempted to show permission dialog for Android < M");
            }
        });
        Button showAppSettingsButton = mView.findViewById(R.id.show_app_settings);
        showAppSettingsButton.setText(EmojiHelper.replaceShortCode(getString(R.string.ui_permissionsMissing_appSettings)));
        showAppSettingsButton.setOnClickListener((View $) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + mContext.getPackageName()));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, REQUEST_CODE_APP_SETTINGS);
            } else {
                throw new RuntimeException("Attempted to show permission dialog for Android < M");
            }
        });
        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(this::continuouslyCheckForPermissions);
    }

    @Override
    public void onResume() {
        super.onResume();

        mPager.setLocked(true);

        if (mHandler == null) {
            mHandler = new Handler();
        }
        continuouslyCheckForPermissions();
    }

    private void continuouslyCheckForPermissions() {
        if (PermissionHelper.granted(mContext)) {
            mHandler.post(() -> {
                mPager.setLocked(false);
                mPager.getScreenAdapter().removePermissionMissingFragment();
            });
            return;
        }
        mHandler.postDelayed(this::continuouslyCheckForPermissions, 500);
    }
}
