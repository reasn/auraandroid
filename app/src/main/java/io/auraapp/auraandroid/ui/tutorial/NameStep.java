package io.auraapp.auraandroid.ui.tutorial;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_NAME_CHANGED_ACTION;

public class NameStep extends TutorialStep {

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context $, Intent intent) {
            mTutorialManager.goTo(TextStep.class);
        }
    };

    public NameStep(ViewGroup mRootView, Context mContext, ScreenPager mPager, TutorialManager manager) {
        super(mRootView, mContext, mPager, manager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_name, mRootView, false);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) screen.findViewById(R.id.tutorial_overlay).getLayoutParams();

        layoutParams.setMargins(0, getRelativeTop(R.id.profile_my_text), 0, 0);

        mPager.goTo(ProfileFragment.class, true);
        mPager.setSwipeLocked(true);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiver,
                IntentFactory.createFilter(LOCAL_MY_PROFILE_NAME_CHANGED_ACTION));

        return screen;
    }

    public void leave() {
        mPager.setSwipeLocked(false);
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return ColorStep.class;
    }
}
