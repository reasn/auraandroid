package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.world.WorldFragment;

public class WorldStep extends TutorialStep {

    private int mMargin;
    private ViewGroup.MarginLayoutParams mRecyclerParams;

    public WorldStep(ViewGroup mRootView, Context mContext, ScreenPager mPager, TutorialManager manager) {
        super(mRootView, mContext, mPager, manager);
    }

    public ViewGroup enter() {
        ViewGroup screen = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.tutorial_world, mRootView, false);
        mPager.setSwipeLocked(false);
        mPager.goTo(WorldFragment.class, true);

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
        return SwipeStep.class;
    }

    @Override
    public Class<? extends TutorialStep> getNextStep() {
        return FinalStep.class;
    }

}
