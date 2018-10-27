package io.auraapp.auraandroid.ui.tutorial;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION;

public class ColorStep extends TutorialStep {

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context $, Intent intent) {
            mTutorialManager.completeCurrentAndGoTo(NameStep.class);
        }
    };


    public ColorStep(ViewGroup mRootView, Context mContext, ScreenPager mPager, TutorialManager manager) {
        super(mRootView, mContext, mPager, manager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_color, mRootView, false);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) screen.findViewById(R.id.tutorial_overlay).getLayoutParams();
        layoutParams.setMargins(0, getRelativeBottom(R.id.profile_color_and_name_wrapper), 0, 0);

        ((TextView) screen.findViewById(R.id.tutorial_color_text)).setText(
                mContext.getResources().getQuantityString(
                        R.plurals.tutorial_color_text,
                        Config.PROFILE_SLOGANS_MAX_SLOGANS,
                        Config.PROFILE_SLOGANS_MAX_SLOGANS
                )
        );

        mPager.goTo(ProfileFragment.class, true);
        mPager.setSwipeLocked(true);


        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiver,
                IntentFactory.createFilter(LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION));

        return screen;
    }

    public void leave() {
        mPager.setSwipeLocked(false);
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return EnabledStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return NameStep.class;
    }
}
