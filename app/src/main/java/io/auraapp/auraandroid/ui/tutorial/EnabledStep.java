package io.auraapp.auraandroid.ui.tutorial;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;

public class EnabledStep extends TutorialStep {

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context $, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }
            CommunicatorProxyState proxyState = (CommunicatorProxyState) extras.getSerializable(IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE);
            if (proxyState != null && proxyState.mEnabled) {
                mTutorialManager.completeCurrentAndGoTo(ColorStep.class);
            }
        }
    };

    public EnabledStep(ViewGroup mRootView, Context mContext, ScreenPager mPager, TutorialManager manager) {
        super(mRootView, mContext, mPager, manager);
    }

    public ViewGroup enter() {

        // EnabledStep automatically progresses to the next step when Aura gets enabled,
        // WelcomeStep doesn't have such logic and therefore needs to be marked as completed
        // explicitly here.
        AuraPrefs.markTutorialStepAsCompleted(mContext, WelcomeStep.class.getName());

        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_enabled, mRootView, false);

        ViewGroup overlay = screen.findViewById(R.id.tutorial_overlay);

        // TODO measure somehow, this didn't work in onResume:
//        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams())
//                .setMargins(0, mRootView.findViewById(R.id.toolbar_fragment).getMeasuredHeight(), 0, 0);
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 55, mContext.getResources().getDisplayMetrics());

        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams())
                .setMargins(0, px, 0, 0);

        mPager.setSwipeLocked(true);
        mPager.goTo(ProfileFragment.class, false);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiver,
                IntentFactory.createFilter(LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION));

        return screen;
    }

    @Override
    public void leave() {
        super.leave();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return WelcomeStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return ColorStep.class;
    }
}
