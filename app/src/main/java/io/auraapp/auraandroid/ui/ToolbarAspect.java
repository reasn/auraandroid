package io.auraapp.auraandroid.ui;


import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
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
import io.auraapp.auraandroid.common.Prefs;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.CommunicatorProxy;
import io.auraapp.auraandroid.ui.common.MySloganManager;
import io.auraapp.auraandroid.ui.debug.DebugFragment;

import static io.auraapp.auraandroid.common.FormattedLog.i;

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
    private DebugFragment mDebugFragment;
    private boolean mDebugFragmentEnabled = false;

    /**
     * This is a member variable to keep it from being garbage collected.
     * Thanks https://stackoverflow.com/questions/2542938/sharedpreferences-onsharedpreferencechangelistener-not-being-called-consistently
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener;
    private SwitchCompat mEnabledSwitch;

    public ToolbarAspect(AppCompatActivity activity,
                         ScreenPager pager,
                         SharedPreferences prefs,
                         CommunicatorProxy communicatorProxy,
                         MySloganManager mySloganManager,
                         Handler handler,
                         DebugFragment debugFragment) {
        this.mActivity = activity;
        this.mPager = pager;
        this.mPrefs = prefs;
        this.mCommunicatorProxy = communicatorProxy;
        this.mMySloganManager = mySloganManager;
        this.mHandler = handler;
        this.mDebugFragment = debugFragment;
    }

    public void initToolbar() {

        mAuraEnabled = mPrefs.getBoolean(Prefs.PREFS_ENABLED, true);

        Toolbar toolbar = mActivity.findViewById(R.id.toolbar);
        mActivity.setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_launcher);
        toolbar.setNavigationOnClickListener($ -> mHandler.post(() -> {
            if (!Config.MAIN_DEBUG_VIEW_ENABLED || mDebugFragmentEnabled) {
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
                mPager.getScreenAdapter().addDebugFragment(mDebugFragment);
                mDebugFragment.update(null, null);
            }
        }));
        mPrefsListener = ($, key) -> {
            if (Prefs.PREFS_COLOR.equals(key)) {
                String color = mPrefs.getString(key, Config.COMMON_DEFAULT_COLOR);
                i(TAG, "Color set to %s", color);
                toolbar.setBackgroundColor(Color.parseColor(color));
                if (mEnabledSwitch != null) {
                    mEnabledSwitch.setTextColor(this.mActivity.getResources().getColor(
                            ColorHelper.getBrightness(Color.parseColor(color)) > 128
                                    ? R.color.black
                                    : R.color.white
                    ));
                }
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
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

        mEnabledSwitch = enabledItem.getActionView().findViewById(R.id.enabled_switch);
        mEnabledSwitch.setChecked(mAuraEnabled);

        // Now that mEnabledSwitch is set we can start coloring toolbar and switch text
        mPrefsListener.onSharedPreferenceChanged(null, Prefs.PREFS_COLOR);

        MainActivity.ToolbarButtonVisibilityUpdater visibilityUpdater = fragment -> {
            // TODO animate / make smoother, first trial didn't work, null pointers and animation wasn't visible

            if (fragment instanceof FragmentWithToolbarButtons) {
                enabledItem.setVisible(true);
            } else {
                enabledItem.setVisible(false);
            }
        };

        mPager.addChangeListener(visibilityUpdater::update);
        visibilityUpdater.update(mPager.getScreenAdapter().getItem(mPager.getCurrentItem()));

        // Managed programmatically because offText XML attribute has no effect for SwitchCompat in menu item
        mEnabledSwitch.setText(mActivity.getString(mAuraEnabled
                ? R.string.ui_toolbar_enable_on
                : R.string.ui_toolbar_enable_off));

        mEnabledSwitch.setOnCheckedChangeListener((CompoundButton $, boolean isChecked) -> {
            mAuraEnabled = isChecked;
            mPrefs.edit().putBoolean(Prefs.PREFS_ENABLED, isChecked).apply();
            if (isChecked) {
                mCommunicatorProxy.enable();
                mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
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
