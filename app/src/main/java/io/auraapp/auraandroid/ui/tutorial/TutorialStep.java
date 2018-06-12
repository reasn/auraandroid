package io.auraapp.auraandroid.ui.tutorial;


import android.content.Context;
import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.ui.ScreenPager;

public abstract class TutorialStep {

    final ViewGroup mRootView;
    final Context mContext;
    final ScreenPager mPager;

    public abstract ViewGroup enter();

    public void leave() {
    }

    abstract public Class<? extends TutorialStep> getPrevious();

    abstract public Class<? extends TutorialStep> getNextStep();

    TutorialStep(ViewGroup mRootView, Context mContext, ScreenPager mPager) {
        this.mRootView = mRootView;
        this.mContext = mContext;
        this.mPager = mPager;
    }

    private int getRelativeTop(View view) {
        if (view.getParent() == mRootView)
            return view.getTop();
        else
            return view.getTop() + getRelativeTop((View) view.getParent());
    }

    int getRelativeTop(@IdRes int viewId) {
        return getRelativeTop(mRootView.findViewById(viewId));
    }

    int getRelativeBottom(@IdRes int viewId) {
        View view = mRootView.findViewById(viewId);
        return getRelativeTop(view) + view.getMeasuredHeight();
    }
}
