package io.auraapp.auraandroid.ui;


import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.Prefs;
import io.auraapp.auraandroid.ui.common.CommunicatorProxy;
import io.auraapp.auraandroid.ui.common.MySloganManager;

import static io.auraapp.auraandroid.common.FormattedLog.w;

public class ToolbarAspect {

    private static final String TAG = "@aura/ui/" + ToolbarAspect.class.getSimpleName();
    private final AppCompatActivity mActivity;
    private final ScreenPager mPager;
    private final SharedPreferences mPrefs;
    private final CommunicatorProxy mCommunicatorProxy;
    private final MySloganManager mMySloganManager;
    private final Handler mHandler;

    private final List<Long> mToolbarIconClicks = new ArrayList<>();

    private boolean mAuraEnabled;

    public ToolbarAspect(AppCompatActivity activity,
                         ScreenPager pager,
                         SharedPreferences prefs,
                         CommunicatorProxy communicatorProxy,
                         MySloganManager mySloganManager,
                         Handler handler) {
        this.mActivity = activity;
        this.mPager = pager;
        this.mPrefs = prefs;
        this.mCommunicatorProxy = communicatorProxy;
        this.mMySloganManager = mySloganManager;
        this.mHandler = handler;
    }

    public void initToolbar() {
        Toolbar toolbar = mActivity.findViewById(R.id.toolbar);
        mActivity.setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_launcher);
        toolbar.setNavigationOnClickListener($ -> mHandler.post(() -> {
            if (!Config.MAIN_DEBUG_VIEW_ENABLED) {
                return;
            }
            long now = System.currentTimeMillis();
            mToolbarIconClicks.add(now);
            w(TAG, "haha %s", mToolbarIconClicks);
            // Iterate over copy to allow modification in loop
            int eligibleClicks = 0;
            for (Long ts : mToolbarIconClicks.toArray(new Long[mToolbarIconClicks.size()])) {
                if (now - ts > Config.MAIN_DEBUG_VIEW_SWITCH_INTERVAL) {
                    mToolbarIconClicks.remove(ts);
                } else {
                    eligibleClicks++;
                }
            }
            if (eligibleClicks >= Config.MAIN_DEBUG_VIEW_SWITCH_CLICKS) {
                mToolbarIconClicks.clear();
//                if (mDebugWrapper.getVisibility() == View.VISIBLE) {
//                    mDebugWrapper.setVisibility(View.GONE);
//                    toast(R.string.ui_main_toast_debug_view_disabled);
//                } else {
//                    toast(R.string.ui_main_toast_debug_view_enabled);
//                    mDebugWrapper.setVisibility(View.VISIBLE);
//                    updateDebugView();
//                }
                // TODO turn into Fragment
            }
        }));
    }

    public void createOptionsMenu(Menu menu) {
        mActivity.getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        MenuItem item = menu.findItem(R.id.enabledSwitch);
        item.setActionView(R.layout.toolbar_switch);

        SwitchCompat enabledSwitch = item.getActionView().findViewById(R.id.enabled_switch);
        enabledSwitch.setChecked(mAuraEnabled);

        MainActivity.ToolbarButtonVisibilityUpdater s = fragment -> {
            if (fragment instanceof FragmentWithToolbarButtons) {
                enabledSwitch.setVisibility(View.VISIBLE);
            } else {
                enabledSwitch.setVisibility(View.INVISIBLE);
            }
        };

        mPager.addChangeListener(s::update);
        s.update(mPager.getScreenAdapter().getItem(mPager.getCurrentItem()));

        // Managed programmatically because offText XML attribute has no effect for SwitchCompat in menu item
        enabledSwitch.setText(mActivity.getString(mAuraEnabled
                ? R.string.ui_toolbar_enable_on
                : R.string.ui_toolbar_enable_off));

        enabledSwitch.setOnCheckedChangeListener((CompoundButton $, boolean isChecked) -> {
            mAuraEnabled = isChecked;
            mPrefs.edit().putBoolean(Prefs.PREFS_ENABLED, isChecked).apply();
            if (isChecked) {
                mCommunicatorProxy.enable();
                mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
                mCommunicatorProxy.askForPeersUpdate();
                enabledSwitch.setText(mActivity.getString(R.string.ui_toolbar_enable_on));
                enabledSwitch.getThumbDrawable().setColorFilter(mActivity.getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                enabledSwitch.getTrackDrawable().setColorFilter(mActivity.getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);

            } else {
                enabledSwitch.setText(mActivity.getString(R.string.ui_toolbar_enable_off));
                mCommunicatorProxy.disable();
                enabledSwitch.getThumbDrawable().setColorFilter(mActivity.getResources().getColor(R.color.red), PorterDuff.Mode.MULTIPLY);
                enabledSwitch.getTrackDrawable().setColorFilter(mActivity.getResources().getColor(R.color.red), PorterDuff.Mode.MULTIPLY);
            }
        });
    }
}
