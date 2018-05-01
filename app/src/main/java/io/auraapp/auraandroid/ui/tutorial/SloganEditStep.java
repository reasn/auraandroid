package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class SloganEditStep extends ProfileStatusHidingStep {

    public SloganEditStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        doEnter();

        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_slogan_edit, mRootView, false);

        screen.findViewById(R.id.tutorial_overlay).getLayoutParams().height = getRelativeBottom(R.id.profile_my_text);

        return screen;
    }

    public void leave() {
        doLeave();
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return SloganAddStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return SwipeStep.class;
    }


}
