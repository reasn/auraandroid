package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class ColorStep extends ProfileStatusHidingStep {

    public ColorStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        doEnter();

        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_color, mRootView, false);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) screen.findViewById(R.id.tutorial_overlay).getLayoutParams();

        layoutParams.setMargins(0, getRelativeTop(R.id.profile_my_text), 0, 0);

        return screen;
    }

    public void leave() {
        doLeave();
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return SwipeStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return NameStep.class;
    }


}
