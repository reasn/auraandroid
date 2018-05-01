package io.auraapp.auraandroid.ui;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.permissions.PermissionsFragment;
import io.auraapp.auraandroid.ui.tutorial.TutorialManager;
import io.auraapp.auraandroid.ui.welcome.TermsFragment;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class ScreenPager extends ViewPager {

    private static final String TAG = "@aura/ui/" + ScreenPager.class.getSimpleName();
    private final Handler mHandler = new Handler();
    private final Timer mTimer = new Timer(mHandler);
    private boolean mLocked;

    public ScreenPager(@NonNull Context context) {
        super(context);
    }

    public ScreenPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mLocked) {
            return false;
        }
        return super.onInterceptTouchEvent(event);
    }

    public boolean redirectIfNeeded(MainActivity activity, @Nullable CommunicatorState state) {
        if ((state != null && !state.mHasPermission)
                || !PermissionHelper.granted(activity)) {
            mHandler.post(() -> {
                getScreenAdapter().addPermissionsFragment();
                goTo(PermissionsFragment.class, false);
            });
            v(TAG, "Scheduled redirect to " + PermissionsFragment.class.getSimpleName());
            return true;
        }

        if (!AuraPrefs.hasAgreedToTerms(activity)) {
            getScreenAdapter().addTermsFragment();
            goTo(TermsFragment.class, false);
            v(TAG, "Scheduled redirect to " + TermsFragment.class.getSimpleName());
            return true;
        }
        if (!AuraPrefs.hasCompletedTutorial(activity)) {
            mHandler.post(() -> activity.getSharedServicesSet().mTutorialManager.open());
            v(TAG, "Scheduled opening " + TutorialManager.class.getSimpleName());
            return true;
        }

        v(TAG, "No redirects required");
        return false;
    }

    @Override
    public void setCurrentItem(int position) {
        setCurrentItem(position, true);
    }

    @Override
    public void setCurrentItem(int position, boolean smoothScroll) {
        v(TAG, "setCurrentItem %d", position);
        getScreenAdapter().sendChangeBroadcast(getScreenAdapter().getItem(position));
        super.setCurrentItem(position, smoothScroll);
    }

    public void setSwipeLocked(boolean locked) {
        if (locked && getScreenAdapter().debugVisible()) {
            // Cannot lock when debug view is enabled
            this.mLocked = false;
        }
        this.mLocked = locked;
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
