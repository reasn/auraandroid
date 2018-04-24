package io.auraapp.auraandroid.ui.common.fragments;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.ui.MainActivity;

import static io.auraapp.auraandroid.common.FormattedLog.v;

abstract public class ContextViewFragment extends ContextFragment {

    private static final String TAG = "@aura/ui/common/" + ContextViewFragment.class.getSimpleName();
    private ViewGroup mRootView;

    @LayoutRes
    abstract protected int getLayoutResource();

    abstract protected void onResumeWithContextAndView(MainActivity activity, ViewGroup rootView);

    protected ViewGroup getRootView() {
        return mRootView;
    }

    @Nullable
    @Override
    final public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v(TAG, "onCreateView, fragment: %s", getClass().getSimpleName());
        mRootView = (ViewGroup) inflater.inflate(getLayoutResource(), container, false);
        return mRootView;
    }

    @Override
    final protected void onResumeWithContext(MainActivity activity) {
        if (mRootView != null) {
            v(TAG, "Invoking onResumeWithContextAndView, fragment: %s", getClass().getSimpleName());
            onResumeWithContextAndView(activity, mRootView);
        }
    }
}
