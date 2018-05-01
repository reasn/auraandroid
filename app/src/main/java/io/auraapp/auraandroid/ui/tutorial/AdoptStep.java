package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.world.WorldFragment;

public class AdoptStep extends TutorialStep {

    public AdoptStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_adopt, mRootView, false);
        mPager.setSwipeLocked(false);
        mPager.goTo(WorldFragment.class, true);
        return screen;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return SwipeStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return null;
    }

}
