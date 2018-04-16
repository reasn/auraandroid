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

    protected void toast(@StringRes int text) {
        Toast.makeText(getContext(), EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
    }

    private boolean mViewAndContextCreatedInvoked = false;

    @LayoutRes
    abstract protected int getLayoutResource();

    abstract protected void onReady(MainActivity activity, ViewGroup rootView);

    protected ViewGroup getRootView() {
        return mRootView;
    }

    private void check(Context context) {

        if (!(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }

        MainActivity activity = (MainActivity) context;

        // Make sure that view exists, MainActivity.onCreate finished and the callback hasn't
        // been invoked within the current lifecycle
        if (getRootView() != null
                && activity.getSharedState() != null
                && !mViewAndContextCreatedInvoked) {
            onReady(activity, getRootView());
            mViewAndContextCreatedInvoked = true;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        check(context);
    }

    @Nullable
    @Override
    final public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v(TAG, "onCreateView, fragment: %s", getClass().getSimpleName());
        mRootView = (ViewGroup) inflater.inflate(getLayoutResource(), container, false);
        check(getContext());
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        check(getContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewAndContextCreatedInvoked = false;
    }
}
