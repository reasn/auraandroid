package io.auraapp.auraandroid.ui.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.CommunicatorProxy;
import io.auraapp.auraandroid.ui.common.fragments.ContextViewFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;
import io.auraapp.auraandroid.ui.settings.SettingsActivity;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_COLOR_CHANGED;

public class ToolbarFragment extends ContextViewFragment {

    private static final String TAG = "@aura/ui/" + ToolbarFragment.class.getSimpleName();
    private Toolbar mToolbar;
    private CommunicatorProxy mCommunicatorProxy;
    private SwitchCompat mEnabledSwitch;
    private Handler mHandler = new Handler();
    private boolean mDebugFragmentEnabled;
    private final List<Long> mToolbarIconClicks = new ArrayList<>();
    private ScreenPager mPager;
    private MyProfileManager mMyProfileManager;

    @Override
    protected int getLayoutResource() {
        return R.layout.main_toolbar_fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResumeWithContextAndView(MainActivity activity, ViewGroup rootView) {

        mCommunicatorProxy = activity.getSharedServicesSet().mCommunicatorProxy;
        mMyProfileManager = activity.getSharedServicesSet().mMyProfileManager;
        mPager = activity.getSharedServicesSet().mPager;

        mToolbar = (Toolbar) rootView;
        activity.setSupportActionBar(mToolbar);

        mToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_tutorial) {
                activity.getSharedServicesSet().mTutorialManager.setCompleted(false);
                activity.getSharedServicesSet().mTutorialManager.open();
                return true;
            }
            if (item.getItemId() == R.id.action_reset_terms) {
                AuraPrefs.putHasAgreedToTerms(activity, false);
                activity.getSharedServicesSet().mTutorialManager.complete();
                activity.getSharedServicesSet().mPager.redirectIfNeeded(activity, null);
                return true;
            }
            if (item.getItemId() == R.id.action_complete_tutorial) {
                activity.getSharedServicesSet().mTutorialManager.complete();
                return true;
            }
            if (item.getItemId() == R.id.action_finish) {
                activity.finish();
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
                        activity,
                        EmojiHelper.replaceShortCode(activity.getString(R.string.ui_main_toast_debug_view_enabled)),
                        Toast.LENGTH_SHORT
                ).show();
                activity.getSharedServicesSet().mPager.getScreenAdapter().addDebugFragment();
            }
        }));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu, menu);

        Context context = getContext();
        if (context == null) {
            return;
        }

        MenuItem enabledItem = menu.findItem(R.id.menu_item_enabled);
        enabledItem.setActionView(R.layout.common_toolbar_switch);

        menu.findItem(R.id.action_reset_terms).setVisible(Config.DEBUG_UI_ENABLED);

        mEnabledSwitch = enabledItem.getActionView().findViewById(R.id.enabled_switch);
        boolean enabled = mCommunicatorProxy.getState().mEnabled;
        mEnabledSwitch.setChecked(enabled);

        // Now that mEnabledSwitch is set we can start coloring toolbar and switch text
        mMyProfileManager.addAndTriggerChangedCallback(new int[]{EVENT_COLOR_CHANGED}, event -> {
            if (event == EVENT_COLOR_CHANGED) {
                String color = mMyProfileManager.getColor();
                i(TAG, "Color set to %s", color);
                mToolbar.setBackgroundColor(Color.parseColor(color));
                mEnabledSwitch.setTextColor(ColorHelper.getTextColor(Color.parseColor(color)));
            }
        });

        // TODO animate / make smoother, first trial didn't work, null pointers and animation wasn't visible
        ScreenPager.ChangeCallback visibilityUpdater = fragment ->
                enabledItem.setVisible(fragment instanceof FragmentWithToolbarButtons);

        mPager.removeChangeListener(visibilityUpdater);
        mPager.addChangeListener(visibilityUpdater);
        visibilityUpdater.onChange(mPager.getScreenAdapter().getItem(mPager.getCurrentItem()));

        // Managed programmatically because offText XML attribute has no effect for SwitchCompat in menu item
        mEnabledSwitch.setText(context.getString(enabled
                ? R.string.ui_toolbar_enable_on
                : R.string.ui_toolbar_enable_off));

        mEnabledSwitch.setOnCheckedChangeListener(($, isChecked) -> {

            if (isChecked) {
                mCommunicatorProxy.enable();
                mCommunicatorProxy.updateMyProfile(mMyProfileManager.getProfile());
                mCommunicatorProxy.askForPeersUpdate();
                mEnabledSwitch.setText(context.getString(R.string.ui_toolbar_enable_on));
                mEnabledSwitch.getThumbDrawable().setColorFilter(context.getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                mEnabledSwitch.getTrackDrawable().setColorFilter(context.getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);

            } else {
                mEnabledSwitch.setText(context.getString(R.string.ui_toolbar_enable_off));
                mCommunicatorProxy.disable();
                mEnabledSwitch.getThumbDrawable().setColorFilter(context.getResources().getColor(R.color.red), PorterDuff.Mode.MULTIPLY);
                mEnabledSwitch.getTrackDrawable().setColorFilter(context.getResources().getColor(R.color.red), PorterDuff.Mode.MULTIPLY);
            }
        });
    }
}
