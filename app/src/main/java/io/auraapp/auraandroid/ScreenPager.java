package io.auraapp.auraandroid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.HashSet;
import java.util.Set;

public class ScreenPager extends ViewPager {

    @FunctionalInterface
    public interface ChangeCallback {
        public void onChange(Fragment fragment);
    }

    private boolean mLocked;

    public ScreenPager(@NonNull Context context) {
        super(context);
        init();
    }

    public ScreenPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private final Set<ChangeCallback> changeCallbacks = new HashSet<>();

    public void addChangeListener(ChangeCallback callback) {
        changeCallbacks.add(callback);
    }

    private void init() {
        // not working after programmatic changes
        addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                for (ChangeCallback changeCallback : changeCallbacks) {
                    changeCallback.onChange(getScreenAdapter().getItem(position));
                }
            }
        });
    }

    @Override
    public void setCurrentItem(int position) {
        super.setCurrentItem(position, true);
        for (ChangeCallback changeCallback : changeCallbacks) {
            changeCallback.onChange(getScreenAdapter().getItem(position));
        }
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
        if (mLocked) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    public void setLocked(boolean locked) {
        this.mLocked = locked;
    }

    public void goTo(Fragment fragment) {
        setCurrentItem(getScreenAdapter().getItemPosition(fragment));
    }

    public void goTo(Class fragmentClass) {
        setCurrentItem(getScreenAdapter().getPosition(fragmentClass));
    }

    public ScreenSlidePagerAdapter getScreenAdapter() {
        return (ScreenSlidePagerAdapter) getAdapter();
    }

}
