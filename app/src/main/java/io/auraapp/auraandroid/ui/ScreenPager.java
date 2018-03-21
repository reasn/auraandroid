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

import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class ScreenPager extends ViewPager {

    private static final String TAG = "@aura/ui/" + ScreenPager.class.getSimpleName();

    private final Set<ChangeCallback> changeCallbacks = new HashSet<>();

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


    public void addChangeListener(ChangeCallback callback) {
        changeCallbacks.add(callback);
    }

    public void removeChangeListener(ChangeCallback callback) {
        changeCallbacks.remove(callback);
    }

    private void init() {
        // not working after programmatic changes
        addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                propagateScreenChange(position);
            }
        });
    }

    private final Handler mHandler = new Handler();

    private void propagateScreenChange(int position) {
        mHandler.postDelayed(() -> {
            Fragment fragment = getScreenAdapter().getItem(position);
            if (fragment instanceof FragmentCameIntoView) {
                ((FragmentCameIntoView) fragment).cameIntoView();
            }
            v(TAG, "setCurrentItem. Invoking change callbacks for %s at %d", fragment, position);
            for (ChangeCallback changeCallback : changeCallbacks) {
                changeCallback.onChange(fragment);
            }
        }, 1000);
    }

    @Override
    public void setCurrentItem(int position) {
        v(TAG, "setCurrentItem %d", position);
        super.setCurrentItem(position, true);
        propagateScreenChange(position);
    }




//
//
//
//    float mStartDragX;
//    OnSwipeOutListener mOnSwipeOutListener;
//
//    public void setOnSwipeOutListener(OnSwipeOutListener listener) {
//        mOnSwipeOutListener = listener;
//    }
//
//    private void onSwipeOutAtStart() {
//        if (mOnSwipeOutListener!=null) {
//            mOnSwipeOutListener.onSwipeOutAtStart();
//        }
//    }
//
//    private void onSwipeOutAtEnd() {
//        if (mOnSwipeOutListener!=null) {
//            mOnSwipeOutListener.onSwipeOutAtEnd();
//        }
//    }
//
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        switch(ev.getAction() & MotionEventCompat.ACTION_MASK){
//            case MotionEvent.ACTION_DOWN:
//                mStartDragX = ev.getX();
//                break;
//        }
//        return super.onInterceptTouchEvent(ev);
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent ev){
//
//        if(getCurrentItem()==0 || getCurrentItem()==getAdapter().getCount()-1){
//            final int action = ev.getAction();
//            float x = ev.getX();
//            switch(action & MotionEventCompat.ACTION_MASK){
//                case MotionEvent.ACTION_MOVE:
//                    break;
//                case MotionEvent.ACTION_UP:
//                    if (getCurrentItem()==0 && x>mStartDragX) {
//                        onSwipeOutAtStart();
//                    }
//                    if (getCurrentItem()==getAdapter().getCount()-1 && x<mStartDragX){
//                        onSwipeOutAtEnd();
//                    }
//                    break;
//            }
//        }else{
//            mStartDragX=0;
//        }
//        return super.onTouchEvent(ev);
//
//    }
//
//    public interface OnSwipeOutListener {
//        void onSwipeOutAtStart();
//        void onSwipeOutAtEnd();
//    }
//
//
//
//






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

    public ScreenSlidePagerAdapter getScreenAdapter() {
        return (ScreenSlidePagerAdapter) getAdapter();
    }

}
