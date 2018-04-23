package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class SwipeStep extends TutorialStep {

    public SwipeStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_swipe, mRootView, false);
        mPager.setSwipeLocked(false);
        return screen;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return EnabledStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return ColorStep.class;
    }

}
