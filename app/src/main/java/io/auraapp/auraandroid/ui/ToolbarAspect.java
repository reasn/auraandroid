package io.auraapp.auraandroid.ui;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.CommunicatorProxy;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;
import io.auraapp.auraandroid.ui.settings.SettingsActivity;

import static android.content.Context.MODE_PRIVATE;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_COLOR_CHANGED;

public class ToolbarAspect {

    private static final String TAG = "@aura/ui/" + ToolbarAspect.class.getSimpleName();
    private final MainActivity mActivity;
    private final SharedPreferences mPrefs;
    private final Handler mHandler;

    private final List<Long> mToolbarIconClicks = new ArrayList<>();
    private final CommunicatorProxy mCommunicatorProxy;

    private boolean mAuraEnabled;
    private boolean mDebugFragmentEnabled = false;

    private SwitchCompat mEnabledSwitch;
    private Toolbar mToolbar;

    public ToolbarAspect(MainActivity activity, CommunicatorProxy communicatorProxy, Handler handler) {
        this.mActivity = activity;
        this.mCommunicatorProxy = communicatorProxy;
        this.mPrefs = activity.getSharedPreferences(Config.PREFERENCES_BUCKET, MODE_PRIVATE);
        ;
        this.mHandler = handler;
    }

    public void initToolbar() {

        mAuraEnabled = mPrefs.getBoolean(mActivity.getString(R.string.prefs_enabled_key), true);

        mToolbar = mActivity.findViewById(R.id.toolbar);
        mActivity.setSupportActionBar(mToolbar);

        mToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                mActivity.startActivity(new Intent(mActivity, SettingsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_tutorial) {
                mActivity.showTutorial();
                return true;
            }
            if (item.getItemId() == R.id.action_terms) {
                mPrefs.edit().putBoolean(mActivity.getString(R.string.prefs_terms_agreed), false).apply();
                mActivity.finish();
                return true;
            }
            return false;
        });

        mToolbar.setOnClickListener($ -> mHandler.post(() -> {
            if (!Config.DEBUG_UI_ENABLED || mDebugFragmentEnabled) {
                return;
            }
            long now = System.currentTimeMillis();
            mToolbarIconClicks.add(now);
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
                mDebugFragmentEnabled = true;
                Toast.makeText(
                        mActivity,
                        EmojiHelper.replaceShortCode(mActivity.getString(R.string.ui_main_toast_debug_view_enabled)),
                        Toast.LENGTH_SHORT
                ).show();
                mActivity.getSharedServicesSet().mPager.getScreenAdapter().addDebugFragment();
            }
        }));
    }

    public boolean isAuraEnabled() {
        return mAuraEnabled;
    }

    public boolean isDebugFragmentEnabled() {
        return mDebugFragmentEnabled;
    }

    public void createOptionsMenu(Menu menu) {
        mActivity.getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        MenuItem enabledItem = menu.findItem(R.id.menu_item_enabled);
        enabledItem.setActionView(R.layout.common_toolbar_switch);

        menu.findItem(R.id.action_terms).setVisible(Config.DEBUG_UI_ENABLED);

        mEnabledSwitch = enabledItem.getActionView().findViewById(R.id.enabled_switch);
        mEnabledSwitch.setChecked(mAuraEnabled);

        ScreenPager pager = mActivity.getSharedServicesSet().mPager;
        MyProfileManager myProfileManager = mActivity.getSharedServicesSet().mMyProfileManager;

        // Now that mEnabledSwitch is set we can start coloring toolbar and switch text
        myProfileManager.addAndTriggerChangedCallback(new int[]{EVENT_COLOR_CHANGED}, event -> {
            if (event == EVENT_COLOR_CHANGED) {
                String color = myProfileManager.getColor();
                i(TAG, "Color set to %s", color);
                mToolbar.setBackgroundColor(Color.parseColor(color));
                mEnabledSwitch.setTextColor(ColorHelper.getTextColor(Color.parseColor(color)));
            }
        });

        MainActivity.ToolbarButtonVisibilityUpdater visibilityUpdater = fragment -> {
            // TODO animate / make smoother, first trial didn't work, null pointers and animation wasn't visible

            if (fragment instanceof FragmentWithToolbarButtons) {
                enabledItem.setVisible(true);
            } else {
                enabledItem.setVisible(false);
            }
        };

        pager.removeChangeListener(visibilityUpdater::update);
        pager.addChangeListener(visibilityUpdater::update);
        visibilityUpdater.update(pager.getScreenAdapter().getItem(pager.getCurrentItem()));

        // Managed programmatically because offText XML attribute has no effect for SwitchCompat in menu item
        mEnabledSwitch.setText(mActivity.getString(mAuraEnabled
                ? R.string.ui_toolbar_enable_on
                : R.string.ui_toolbar_enable_off));

        mEnabledSwitch.setOnCheckedChangeListener((CompoundButton $, boolean isChecked) -> {
            mAuraEnabled = isChecked;
            mPrefs.edit().putBoolean(mActivity.getString(R.string.prefs_enabled_key), isChecked).apply();
            if (isChecked) {
                mCommunicatorProxy.enable();
                mCommunicatorProxy.updateMyProfile(myProfileManager.getProfile());
                mCommunicatorProxy.askForPeersUpdate();
                mEnabledSwitch.setText(mActivity.getString(R.string.ui_toolbar_enable_on));
                mEnabledSwitch.getThumbDrawable().setColorFilter(mActivity.getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                mEnabledSwitch.getTrackDrawable().setColorFilter(mActivity.getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);

            } else {
                mEnabledSwitch.setText(mActivity.getString(R.string.ui_toolbar_enable_off));
                mCommunicatorProxy.disable();
                mEnabledSwitch.getThumbDrawable().setColorFilter(mActivity.getResources().getColor(R.color.red), PorterDuff.Mode.MULTIPLY);
                mEnabledSwitch.getTrackDrawable().setColorFilter(mActivity.getResources().getColor(R.color.red), PorterDuff.Mode.MULTIPLY);
            }
        });
    }
}
