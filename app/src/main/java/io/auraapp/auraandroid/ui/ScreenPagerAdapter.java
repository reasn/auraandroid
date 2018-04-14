package io.auraapp.auraandroid.ui;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.ui.debug.DebugFragment;
import io.auraapp.auraandroid.ui.permissions.PermissionsFragment;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;
import io.auraapp.auraandroid.ui.tutorial.TutorialManager;
import io.auraapp.auraandroid.ui.welcome.TermsFragment;
import io.auraapp.auraandroid.ui.welcome.WelcomeFragment;
import io.auraapp.auraandroid.ui.world.WorldFragment;

public class ScreenPagerAdapter extends FragmentStatePagerAdapter {
    public static final String SCREEN_PRIVACY = "privacy";
    public static final String SCREEN_WELCOME = "welcome";
    private static final String SCREEN_WORLD = "world";
    private static final String SCREEN_PROFILE = "profile";
    private final ScreenPager mPager;
    private final Activity mActivity;
    private final List<Fragment> mFragments;
    private final TutorialManager mTutorialManager;

    public ScreenPagerAdapter(FragmentManager fm,
                              ProfileFragment profileFragment,
                              WorldFragment worldFragment,
                              ScreenPager pager,
                              TutorialManager tutorialManager,
                              Activity activity) {
        super(fm);
        this.mPager = pager;
        mActivity = activity;
        mTutorialManager = tutorialManager;

        mFragments = new ArrayList<>();
        mFragments.add(profileFragment);
        mFragments.add(worldFragment);
    }

    private boolean has(Class fragmentClass) {
        for (Fragment fragment : mFragments) {
            if (fragment.getClass().equals(fragmentClass)) {
                return true;
            }
        }
        return false;
    }

    public Class getClassForHandle(String handle) {
        if (handle.equals(SCREEN_PROFILE)) {
            return ProfileFragment.class;
        }
        if (handle.equals(SCREEN_WORLD)) {
            return WorldFragment.class;
        }
        if (handle.equals(SCREEN_WELCOME)) {
            return WelcomeFragment.class;
        }
        return TermsFragment.class;
    }

    public String getHandleForClass(Class fragment) {
        if (fragment.equals(ProfileFragment.class)) {
            return SCREEN_PROFILE;
        }
        if (fragment.equals(WorldFragment.class)) {
            return SCREEN_WORLD;
        }
        if (fragment.equals(TermsFragment.class)) {
            return SCREEN_WELCOME;
        }
        return SCREEN_PRIVACY;
    }

    public void addPermissionsFragment() {

        if (!has(PermissionsFragment.class)) {
            mFragments.add(0, PermissionsFragment.create(mActivity, mPager));
            notifyDataSetChanged();
        }
    }

    public void addDebugFragment(DebugFragment fragment) {
        if (!has(DebugFragment.class)) {
            mFragments.add(fragment);
            notifyDataSetChanged();
        }
    }

    public void addWelcomeFragments() {
        boolean added = false;
        if (!has(WelcomeFragment.class)) {
            mFragments.add(0, WelcomeFragment.create(mActivity, mPager, mTutorialManager));
            added = true;
        }
        if (!has(TermsFragment.class)) {
            mFragments.add(0, TermsFragment.create(mActivity, mPager));
            added = true;
        }

        if (added) {
            notifyDataSetChanged();
        }
    }

    public void removeWelcomeFragments() {
        boolean removed = false;
        for (Fragment fragment : mFragments.toArray(new Fragment[mFragments.size()])) {
            if (fragment instanceof WelcomeFragment || fragment instanceof TermsFragment) {
                mFragments.remove(fragment);
                removed = true;
            }
        }
        if (removed) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if (!(object instanceof Fragment)) {
            throw new RuntimeException("Tried to getItemPosition of non-Fragment");
        }
//        return PagerAdapter.POSITION_NONE;
        // Didn't work:
        int index = mFragments.indexOf(object);
        return index >= 0
                ? index
                : PagerAdapter.POSITION_NONE;
        // The current solution might have performance implications but none were observed.
        // Would affect swipes only anyway and all screens' views are inflated continuously anyway
        // Thanks https://stackoverflow.com/questions/10849552/update-viewpager-dynamically
    }

    public int getPosition(Class fragmentClass) {
        for (int i = 0; i < mFragments.size(); i++) {
            if (mFragments.get(i).getClass().equals(fragmentClass)) {
                return i;
            }
        }
        throw new RuntimeException("Fragment with class not found: " + fragmentClass.getSimpleName());
    }

    @Override
    public Fragment getItem(int position) {
        if (mFragments.size() <= position) {
            throw new RuntimeException("No fragment registered for position " + position);
        }
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }
}
