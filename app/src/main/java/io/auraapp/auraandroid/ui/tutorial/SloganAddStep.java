package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.Dimensions;

public class SloganAddStep extends ProfileStatusHidingStep {

    public SloganAddStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        super(mRootView, mContext, mPager);
    }

    public ViewGroup enter() {
        doEnter();

        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_slogan_add, mRootView, false);

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) screen.findViewById(R.id.tutorial_overlay).getLayoutParams();

        mRootView.findViewById(R.id.pager).getLayoutParams().height = Dimensions.dp2px(300, mContext);
        layoutParams.setMargins(0, Dimensions.dp2px(340, mContext), 0, 0);

//        layoutParams.height = getRelativeTop(R.id.profile_add_slogan)
//                - ((ViewGroup.MarginLayoutParams) mRootView.findViewById(R.id.profile_add_slogan).getLayoutParams()).bottomMargin;


        return screen;
    }

    public void leave() {
        doLeave();
        mRootView.findViewById(R.id.pager).getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return TextStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return SloganEditStep.class;
    }


}
