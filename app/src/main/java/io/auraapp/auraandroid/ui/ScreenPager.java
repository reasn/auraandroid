package io.auraapp.auraandroid.ui;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class ScreenPager extends ViewPager {

    private static final String TAG = "@aura/ui/" + ScreenPager.class.getSimpleName();

    private final Set<ChangeCallback> changeCallbacks = new HashSet<>();

    private final Handler mHandler = new Handler();
    private final Timer mTimer = new Timer(mHandler);

    @FunctionalInterface
    public interface ChangeCallback {
        public void onChange(Fragment fragment);
    }

    private Timer.Timeout mItemWatchTimeout;
    private boolean mLocked;
    private int mCurrentItem = -1;
    private int mWatchRuns = 0;

    public ScreenPager(@NonNull Context context) {
        super(context);
    }

    public ScreenPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void addChangeListener(ChangeCallback callback) {
        changeCallbacks.add(callback);
    }

    public void removeChangeListener(ChangeCallback callback) {
        changeCallbacks.remove(callback);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mLocked) {
            return false;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            startWatching();
        }
        if (mLocked) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    /**
     * The standard way of detecting fragment changes is addOnPageChangeListener().
     * That sometimes takes seconds to fire making view changes feel disconnected.
     * This is a dirty workaround as it polls the current state after the user finished
     * swiping or setCurrentItem() has been called programmatically.
     */
    private void startWatching() {
        mHandler.post(() -> {
            Timer.clear(mItemWatchTimeout);
            mWatchRuns = 10;
            keepWatching();
        });
    }

    private void keepWatching() {
        int newCurrentItem = getCurrentItem();
        if (mCurrentItem != newCurrentItem) {
            mCurrentItem = newCurrentItem;
            propagateScreenChange(mCurrentItem);
        } else if (--mWatchRuns > 0) {
            Timer.clear(mItemWatchTimeout);
            mItemWatchTimeout = mTimer.setTimeout(this::keepWatching, 200);
        }
    }

    private void propagateScreenChange(int position) {
        Fragment fragment = getScreenAdapter().getItem(position);
        if (fragment instanceof FragmentCameIntoView) {
            ((FragmentCameIntoView) fragment).cameIntoView();
        }
        v(TAG, "setCurrentItem. Invoking change callbacks for %s at %d", fragment, position);
        for (ChangeCallback changeCallback : changeCallbacks) {
            changeCallback.onChange(fragment);
        }
    }

    @Override
    public void setCurrentItem(int position) {
        setCurrentItem(position, true);
    }

    @Override
    public void setCurrentItem(int position, boolean smoothScroll) {
        v(TAG, "setCurrentItem %d", position);
        startWatching();
        super.setCurrentItem(position, smoothScroll);
    }

    public void setLocked(boolean locked) {
        this.mLocked = locked;
    }

    public void goTo(Fragment fragment, boolean smoothScroll) {
        int index = getScreenAdapter().getItemPosition(fragment);
        i(TAG, "Going to fragment %s at %d", fragment.getClass().getSimpleName(), index);
        setCurrentItem(index > 0 ? index : 0, smoothScroll);
    }

    public void goTo(Class fragmentClass, boolean smoothScroll) {
        int index = getScreenAdapter().getPosition(fragmentClass);
        i(TAG, "Going to fragment %s at %d", fragmentClass.getSimpleName(), index);
        setCurrentItem(index > 0 ? index : 0, smoothScroll);
    }

    public ScreenPagerAdapter getScreenAdapter() {
        return (ScreenPagerAdapter) getAdapter();
    }

}
