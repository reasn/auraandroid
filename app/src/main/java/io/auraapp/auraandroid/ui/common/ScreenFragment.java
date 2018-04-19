package io.auraapp.auraandroid.ui.common;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.ui.MainActivity;

import static io.auraapp.auraandroid.common.FormattedLog.v;

abstract public class ScreenFragment extends Fragment {

    private static final String TAG = "@aura/ui/common/" + ScreenFragment.class.getSimpleName();
    private ViewGroup mRootView;

    @LayoutRes
    abstract protected int getLayoutResource();

    abstract protected void onResumeWithContext(MainActivity activity, ViewGroup rootView);

    protected void onPauseWithContext(MainActivity activity) {
    }

    protected ViewGroup getRootView() {
        return mRootView;
    }

    public MainActivity getMainActivity() {
        Context context = getContext();
        if (context != null && !(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }
        return (MainActivity) getContext();
    }

    @Nullable
    @Override
    final public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v(TAG, "onCreateView, fragment: %s", getClass().getSimpleName());
        mRootView = (ViewGroup) inflater.inflate(getLayoutResource(), container, false);
        return mRootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = getMainActivity();

        // Make sure that view exists, MainActivity.onCreate finished and the callback hasn't
        // been invoked within the current lifecycle
        if (mRootView != null && activity.getSharedServicesSet() != null) {
            v(TAG, "Invoking onResumeWithContext, fragment: %s", getClass().getSimpleName());
            onResumeWithContext(activity, mRootView);
        }
    }

    @Override
    public void onPause() {
        v(TAG, "onPause, fragment: %s", getClass().getSimpleName());
        if (getContext() != null) {
            onPauseWithContext(getMainActivity());
        }
        super.onPause();
    }

    protected void toast(@StringRes int text) {
        Toast.makeText(getContext(), EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
    }
}
