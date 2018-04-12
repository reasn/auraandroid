package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

abstract class ProfileStatusHidingStep extends TutorialStep {

    private int mInfoBoxHeight;
    private int mSummaryHeight;

    // TODO hide profile_my_slogans_info_box
    ProfileStatusHidingStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    void doEnter() {
        // Hacky way of hiding stuff. But allows to keep the fragment agnostic of tutorial logic
        View summary = mRootView.findViewById(R.id.profile_status_summary);
        View infoBox = mRootView.findViewById(R.id.profile_status_info_box);
        mInfoBoxHeight = infoBox.getLayoutParams().height;
        mSummaryHeight = summary.getLayoutParams().height;
        infoBox.getLayoutParams().height = 0;
        summary.getLayoutParams().height = 0;

        mPager.goTo(ProfileFragment.class, true);
        mPager.setLocked(true);
    }

    void doLeave() {
        mRootView.findViewById(R.id.profile_status_info_box).getLayoutParams().height = mInfoBoxHeight;
        mRootView.findViewById(R.id.profile_status_summary).getLayoutParams().height = mSummaryHeight;

        mPager.setLocked(false);
    }
}
