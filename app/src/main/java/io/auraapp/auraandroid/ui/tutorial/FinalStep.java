package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class FinalStep extends TutorialStep {

    public FinalStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_final, mRootView, false);
        ViewGroup overlay = screen.findViewById(R.id.tutorial_overlay);
        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams()).setMargins(0, 0, 0, 0);

        return screen;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return WorldStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return null;
    }
}
