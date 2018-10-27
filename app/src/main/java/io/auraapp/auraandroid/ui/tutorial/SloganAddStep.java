package io.auraapp.auraandroid.ui.tutorial;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.Dimensions;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_ADOPTED_ACTION;

public class SloganAddStep extends TutorialStep {

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context $, Intent intent) {
            mTutorialManager.completeCurrentAndGoTo(SwipeStep.class);
        }
    };

    public SloganAddStep(ViewGroup mRootView, Context mContext, ScreenPager mPager, TutorialManager manager) {
        super(mRootView, mContext, mPager, manager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_slogan_add, mRootView, false);

        mRootView.findViewById(R.id.profile_my_text).setVisibility(View.GONE);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) screen.findViewById(R.id.tutorial_overlay).getLayoutParams();

        mRootView.findViewById(R.id.pager).getLayoutParams().height = Dimensions.dp2px(300, mContext);
        layoutParams.setMargins(0, Dimensions.dp2px(340, mContext), 0, 0);

//        layoutParams.height = getRelativeTop(R.id.profile_add_slogan)
//                - ((ViewGroup.MarginLayoutParams) mRootView.findViewById(R.id.profile_add_slogan).getLayoutParams()).bottomMargin;


        mPager.goTo(ProfileFragment.class, true);
        mPager.setSwipeLocked(true);

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiver,
                IntentFactory.createFilter(LOCAL_MY_PROFILE_ADOPTED_ACTION));

        return screen;
    }

    public void leave() {
        mPager.setSwipeLocked(false);
        mRootView.findViewById(R.id.profile_my_text).setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.pager).getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return TextStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return SwipeStep.class;
    }


}
