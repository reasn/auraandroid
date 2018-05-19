package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

public class NameStep extends TutorialStep {

    public NameStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_name, mRootView, false);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) screen.findViewById(R.id.tutorial_overlay).getLayoutParams();

        layoutParams.setMargins(0, getRelativeTop(R.id.profile_my_text), 0, 0);

        mPager.goTo(ProfileFragment.class, true);
        mPager.setSwipeLocked(true);
        return screen;
    }

    public void leave() {
        mPager.setSwipeLocked(false);
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return ColorStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return TextStep.class;
    }


}
