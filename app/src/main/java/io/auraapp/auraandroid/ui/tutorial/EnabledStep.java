package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

public class EnabledStep extends TutorialStep {

    public EnabledStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_enabled, mRootView, false);

        ViewGroup overlay = screen.findViewById(R.id.tutorial_overlay);

        // TODO measure somehow, this didn't work in onResume:
//        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams())
//                .setMargins(0, mRootView.findViewById(R.id.toolbar_fragment).getMeasuredHeight(), 0, 0);
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 55, mContext.getResources().getDisplayMetrics());

        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams())
                .setMargins(0, px, 0, 0);

        mPager.setSwipeLocked(true);
        mPager.goTo(ProfileFragment.class, false);

        return screen;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return WelcomeStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return ColorStep.class;
    }
}
