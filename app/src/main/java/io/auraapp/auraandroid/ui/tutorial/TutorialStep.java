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
    final protected TutorialManager mTutorialManager;


    TutorialStep(ViewGroup rootView, Context context, ScreenPager pager, TutorialManager manager) {
        mRootView = rootView;
        mContext = context;
        mPager = pager;
        mTutorialManager = manager;
    }

    public abstract ViewGroup enter();

    abstract public Class<? extends TutorialStep> getPrevious();

    public void leave() {
    }

    public Class<? extends TutorialStep> getNextStep() {
        return null;
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
