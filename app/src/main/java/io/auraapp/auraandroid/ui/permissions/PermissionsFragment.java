package io.auraapp.auraandroid.ui.permissions;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.fragments.ContextViewFragment;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class PermissionsFragment extends ContextViewFragment {

    private static final String TAG = "@aura/ui/permissions/" + PermissionsFragment.class.getSimpleName();
    private static final int REQUEST_CODE_LOCATION_REQUEST = 149;
    private static final int REQUEST_CODE_APP_SETTINGS = 144;
    public static final long FRAGMENT_ID = 5032;

    private final Handler mHandler = new Handler();
    private final Timer mTimer = new Timer(mHandler);
    private Timer.Timeout mCheckTimeout;
    private ScreenPager mPager;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newFragmentClass = intent.getStringExtra(IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_EXTRA_NEW);
            if (PermissionsFragment.class.toString().equals(newFragmentClass)) {
                mPager.setSwipeLocked(true);
            }
            String previousFragmentClass = intent.getStringExtra(IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_EXTRA_PREVIOUS);
            if (mPager != null && PermissionsFragment.class.toString().equals(previousFragmentClass)) {
                mPager.getScreenAdapter().remove(PermissionsFragment.class);
            }
        }
    };

    @Override
    protected int getLayoutResource() {
        return R.layout.permissions_fragment;
    }

    @Override
    protected void onResumeWithContextAndView(MainActivity activity, ViewGroup rootView) {

        mPager = activity.getSharedServicesSet().mPager;
        LocalBroadcastManager.getInstance(activity).registerReceiver(
                mReceiver,
                IntentFactory.createFilter(IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_ACTION)
        );
        mPager.setSwipeLocked(true);
        mHandler.post(() -> continuouslyCheckForPermissions(activity));

        rootView.findViewById(R.id.permissions_grant_permission).setOnClickListener($ -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_REQUEST);
            } else {
                throw new RuntimeException("Attempted to show permission dialog for Android < M");
            }
        });
        rootView.findViewById(R.id.permissions_show_app_settings).setOnClickListener($ -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                throw new RuntimeException("Attempted to show application settings for Android < M");
            }
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()));
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(intent, REQUEST_CODE_APP_SETTINGS);
        });
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mReceiver);
        Timer.clear(mCheckTimeout);
    }

    private void continuouslyCheckForPermissions(MainActivity activity) {
        Timer.clear(mCheckTimeout);
        Context context = getContext();
        if (context == null || !PermissionHelper.granted(context)) {
            mCheckTimeout = mTimer.setTimeout(() -> continuouslyCheckForPermissions(activity), 500);
            return;
        }
        mPager.setSwipeLocked(false);
        if (!mPager.redirectIfNeeded(activity, null)) {
            // no redirect happened, let's "start"
            mPager.goTo(ProfileFragment.class, true);
        }
    }
}
