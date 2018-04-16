package io.auraapp.auraandroid.ui.permissions;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.ScreenFragment;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PermissionsFragment extends ScreenFragment {

    private static final String TAG = "@aura/ui/permissions/" + PermissionsFragment.class.getSimpleName();

    private final static int REQUEST_CODE_LOCATION_REQUEST = 149;
    private static final int REQUEST_CODE_APP_SETTINGS = 144;
    private Handler mHandler;
    private ScreenPager mPager;
    private boolean mRedirected = false;

    @Override
    protected int getLayoutResource() {
        return R.layout.permissions_fragment;
    }

    @Override
    protected void onReady(MainActivity activity, ViewGroup rootView) {

        mPager = activity.getSharedServicesSet().mPager;

        InfoBox infoBox = rootView.findViewById(R.id.info_box);
        infoBox.setButtonClickListener($ -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_REQUEST);
            } else {
                throw new RuntimeException("Attempted to show permission dialog for Android < M");
            }
        });
        Button showAppSettingsButton = rootView.findViewById(R.id.show_app_settings);
        showAppSettingsButton.setText(EmojiHelper.replaceShortCode(getString(R.string.ui_permissions_appSettings)));
        showAppSettingsButton.setOnClickListener($ -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, REQUEST_CODE_APP_SETTINGS);
            } else {
                throw new RuntimeException("Attempted to show permission dialog for Android < M");
            }
        });

        ((TextView) rootView.findViewById(R.id.granted_emoji)).setText(EmojiHelper.replaceShortCode(":grinning_face:"));
        ((TextView) rootView.findViewById(R.id.granted_text)).setText(EmojiHelper.replaceShortCode(activity.getString(R.string.ui_permissions_granted_text)));

        if (mRedirected) {
            rootView.findViewById(R.id.not_granted).setVisibility(View.GONE);
        } else {
            rootView.findViewById(R.id.not_granted).setVisibility(View.VISIBLE);
        }
    }

    @Override
    @ExternalInvocation
    public void onResume() {
        v(TAG, "onResume");
        super.onResume();
        if (!mRedirected) {
            mPager.setLocked(true);

            if (mHandler == null) {
                mHandler = new Handler();
            }
            continuouslyCheckForPermissions();
        }
    }

    @Override
    public void onPause() {
        v(TAG, "onPause");
        super.onPause();
        mHandler.removeCallbacks(this::continuouslyCheckForPermissions);
    }


    private void continuouslyCheckForPermissions() {
        if (!PermissionHelper.granted(getContext())) {
            mHandler.postDelayed(this::continuouslyCheckForPermissions, 500);
            return;
        }
        mRedirected = true;
        // Give dialog time to hide, leads to glitches otherwise
        mHandler.postDelayed(() -> {
            i(TAG, "Permissions granted");

            Animation hide = AnimationUtils.loadAnimation(getContext(), R.anim.screen_permissions_hide);
            getRootView().findViewById(R.id.not_granted).startAnimation(hide);

            mHandler.postDelayed(() -> {
                getRootView().findViewById(R.id.not_granted).setVisibility(View.GONE);
                mPager.setLocked(false);
            }, hide.getDuration());
        }, 500);
    }
}
