package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class EnabledStep extends TutorialStep {

    public EnabledStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_enabled, mRootView, false);

        ViewGroup overlay = screen.findViewById(R.id.tutorial_overlay);
        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams())
                .setMargins(0, mRootView.findViewById(R.id.toolbar).getMeasuredHeight(), 0, 0);

        mPager.setLocked(true);

        return screen;
    }

    @Override
    public void leave() {
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return null;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return SwipeStep.class;
    }
}
