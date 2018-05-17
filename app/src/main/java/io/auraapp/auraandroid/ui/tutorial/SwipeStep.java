package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class SwipeStep extends TutorialStep {

    private int mMargin;

    public SwipeStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_swipe, mRootView, false);
        mPager.setSwipeLocked(false);
        // TODO let steps control where to inject tutorial to support a wider varierty of screens
        mMargin = ((ViewGroup.MarginLayoutParams) mRootView.findViewById(R.id.world_slogans_recycler).getLayoutParams()).bottomMargin;
        ((ViewGroup.MarginLayoutParams) mRootView.findViewById(R.id.world_slogans_recycler).getLayoutParams()).bottomMargin = 300;
        return screen;
    }

    @Override
    public void leave() {
        super.leave();
        ((ViewGroup.MarginLayoutParams) mRootView.findViewById(R.id.world_slogans_recycler).getLayoutParams()).bottomMargin = mMargin;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return SloganEditStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return AdoptStep.class;
    }

}
