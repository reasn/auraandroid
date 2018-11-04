package io.auraapp.auraandroid.ui.tutorial;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.ui.ScreenPager;

import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_ACTION;

public class SwipeStep extends TutorialStep {

    private int mMargin;
    private ViewGroup.MarginLayoutParams mRecyclerParams;
    private View mNextButton;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context $, Intent intent) {
            AuraPrefs.markTutorialStepAsCompleted(mContext, SwipeStep.class.getName());
            if (mNextButton != null) {
                mNextButton.setVisibility(View.VISIBLE);
            }
        }
    };

    public SwipeStep(ViewGroup mRootView, Context mContext, ScreenPager mPager, TutorialManager manager) {
        super(mRootView, mContext, mPager, manager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_swipe, mRootView, false);
        mNextButton = screen.findViewById(R.id.tutorial_next);
        mPager.setSwipeLocked(false);
        mRecyclerParams = ((ViewGroup.MarginLayoutParams) mRootView.findViewById(R.id.world_peers_recycler).getLayoutParams());
        mMargin = mRecyclerParams.bottomMargin;
        screen.post(() -> mRecyclerParams.bottomMargin = screen.findViewById(R.id.tutorial_overlay).getMeasuredHeight());

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiver,
                IntentFactory.createFilter(LOCAL_SCREEN_PAGER_CHANGED_ACTION));

        return screen;
    }

    @Override
    public void leave() {
        super.leave();
        mRecyclerParams.bottomMargin = mMargin;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return SloganAddStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return WorldStep.class;
    }
}
