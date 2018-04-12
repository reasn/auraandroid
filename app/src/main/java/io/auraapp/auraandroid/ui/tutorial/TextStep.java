package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class TextStep extends ProfileStatusHidingStep {

    public TextStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        doEnter();

        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_text, mRootView, false);

        ViewGroup overlay = screen.findViewById(R.id.tutorial_overlay);

        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams())
                .setMargins(0, getRelativeTop(R.id.profile_slogans_recycler), 0, 0);

        return screen;
    }

    public void leave() {
        doLeave();
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return NameStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return null;
    }


}
