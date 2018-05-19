package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class WelcomeStep extends TutorialStep {

    public WelcomeStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_welcome, mRootView, false);
        ViewGroup overlay = screen.findViewById(R.id.tutorial_overlay);
        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams()).setMargins(0, 0, 0, 0);
        mPager.setSwipeLocked(true);
//        ((TextView) screen.findViewById(R.id.tutorial_welcome_text)).setText(Html.fromHtml(mContext.getString(R.string.tutorial_welcome_text), FROM_HTML_MODE_COMPACT));
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
        return EnabledStep.class;
    }
}
