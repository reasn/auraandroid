package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.ui.ScreenPager;

public class ColorStep extends ProfileStatusHidingStep {

    public ColorStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        doEnter();

        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_color, mRootView, false);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) screen.findViewById(R.id.tutorial_overlay).getLayoutParams();

        layoutParams.setMargins(0, getRelativeBottom(R.id.profile_color_and_name_wrapper), 0, 0);

        ((TextView) screen.findViewById(R.id.tutorial_color_text)).setText(
                mContext.getResources().getQuantityString(
                        R.plurals.tutorial_color_text,
                        Config.PROFILE_SLOGANS_MAX_SLOGANS,
                        Config.PROFILE_SLOGANS_MAX_SLOGANS
                )
        );

        return screen;
    }

    public void leave() {
        doLeave();
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return EnabledStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return NameStep.class;
    }


}
