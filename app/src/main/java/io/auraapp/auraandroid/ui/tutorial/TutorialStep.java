package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.ui.ScreenPager;

public abstract class TutorialStep {

    public static final int STEP_ENABLED = 1;
    public static final int STEP_SWIPE = 2;
    public static final int STEP_COLOR = 3;
    public static final int STEP_NAME = 4;
    protected ViewGroup mRootView;
    protected Context mContext;
    protected ScreenPager mPager;

    public abstract ViewGroup enter();

    public void leave() {
    }

    abstract public Class<? extends TutorialStep> getPrevious();

    abstract public Class<? extends TutorialStep> getNextStep();

    public TutorialStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        this.mRootView = mRootView;
        this.mContext = mContext;
        this.mPager = mPager;
    }


    int getRelativeTop(View view) {
        if (view.getParent() == mRootView)
            return view.getTop();
        else
            return view.getTop() + getRelativeTop((View) view.getParent());
    }
    int getRelativeTop(@IdRes int viewId) {
        return getRelativeTop(mRootView.findViewById(viewId));
    }
}
