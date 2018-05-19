package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

public class TextStep extends TutorialStep {

    public TextStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_text, mRootView, false);

        ViewGroup overlay = screen.findViewById(R.id.tutorial_overlay);

        mRootView.findViewById(R.id.profile_scroll_view).scrollTo(0, 0);
        // Disables scrolling
        // Thanks https://stackoverflow.com/questions/18652692/how-to-disable-scrollview-scrolling
        mRootView.findViewById(R.id.profile_scroll_view).setOnTouchListener((v, event) -> true);

        ((ViewGroup.MarginLayoutParams) overlay.getLayoutParams())
                .setMargins(0, getRelativeBottom(R.id.profile_my_text), 0, 0);

        mPager.goTo(ProfileFragment.class, true);
        mPager.setSwipeLocked(true);
        return screen;
    }

    public void leave() {
        mPager.setSwipeLocked(false);
        mRootView.findViewById(R.id.profile_scroll_view).setOnTouchListener(null);
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return NameStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return SloganAddStep.class;
    }


}
