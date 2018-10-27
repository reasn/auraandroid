package io.auraapp.auraandroid.ui.tutorial;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

public class SwipeStep extends TutorialStep {

    private int mMargin;
    private ViewGroup.MarginLayoutParams mRecyclerParams;

    public SwipeStep(ViewGroup mRootView, Context mContext, ScreenPager mPager, TutorialManager manager) {
        super(mRootView, mContext, mPager, manager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_swipe, mRootView, false);
        mPager.setSwipeLocked(false);
        mRecyclerParams = ((ViewGroup.MarginLayoutParams) mRootView.findViewById(R.id.world_slogans_recycler).getLayoutParams());
        mMargin = mRecyclerParams.bottomMargin;
        screen.post(() -> mRecyclerParams.bottomMargin = screen.findViewById(R.id.tutorial_overlay).getMeasuredHeight());
        return screen;
    }

    @Override
    public void leave() {
        super.leave();
        mRecyclerParams.bottomMargin = mMargin;
    }

    @Override
    public Class<? extends TutorialStep> getPrevious() {
        return SloganAddStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return WorldStep.class;
    }
}
