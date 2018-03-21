package io.auraapp.auraandroid.ui.permissions;

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
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.InfoBox;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PermissionMissingFragment extends Fragment {

    private static final String TAG = "@aura/ui/permissions/" + PermissionMissingFragment.class.getSimpleName();

    private final static int REQUEST_CODE_LOCATION_REQUEST = 149;
    private static final int REQUEST_CODE_APP_SETTINGS = 144;
    private Handler mHandler;
    private Context mContext;
    private ScreenPager mPager;
    private ViewGroup mView;
    private boolean redirected = false;

    public static PermissionMissingFragment create(Context context, ScreenPager pager) {
        PermissionMissingFragment fragment = new PermissionMissingFragment();
        fragment.mContext = context;
        fragment.mPager = pager;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

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
        v(TAG, "onPause");
        super.onPause();
        mHandler.removeCallbacks(this::continuouslyCheckForPermissions);
    }

    @Override
    public void onResume() {
        v(TAG, "onResume");
        super.onResume();
        if (!redirected) {
            mPager.setLocked(true);

            if (mHandler == null) {
                mHandler = new Handler();
            }
            continuouslyCheckForPermissions();
        }
    }

    private void continuouslyCheckForPermissions() {
        if (PermissionHelper.granted(mContext)) {
            redirected = true;
            mHandler.post(() -> {
                i(TAG, "Permission granted, removing fragment");
                mPager.setLocked(false);
                mPager.goTo(PermissionGrantedFragment.class, true);
//                mPager.getScreenAdapter().removePermissionMissingFragment();
            });
            return;
        }
        mHandler.postDelayed(this::continuouslyCheckForPermissions, 500);
    }
}
