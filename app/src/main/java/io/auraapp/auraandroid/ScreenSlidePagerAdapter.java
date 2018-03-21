package io.auraapp.auraandroid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.tutorial.PermissionGrantedFragment;
import io.auraapp.auraandroid.tutorial.PermissionMissingFragment;
import io.auraapp.auraandroid.tutorial.ProfileFragment;
import io.auraapp.auraandroid.tutorial.WelcomeFragment;
import io.auraapp.auraandroid.tutorial.WorldFragment;

public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
    public static final String SCREEN_WELCOME = "welcome";
    private static final String SCREEN_WORLD = "world";
    private static final String SCREEN_PROFILE = "profile";
    private final ScreenPager mPager;
    private final Context mContext;
    private final List<Fragment> mFragments;

    public ScreenSlidePagerAdapter(FragmentManager fm, WorldFragment worldFragment, ScreenPager pager, Context context) {
        super(fm);
        this.mPager = pager;
        mContext = context;

        mFragments = new ArrayList<>();
        mFragments.add(new WelcomeFragment());
        mFragments.add(new ProfileFragment());
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
        if (mFragments.equals(SCREEN_WORLD)) {
            return WorldFragment.class;
        }
        return WelcomeFragment.class;
    }

    public String getHandleForClass(Class fragment) {
        if (fragment.equals(ProfileFragment.class)) {
            return SCREEN_PROFILE;
        }
        if (fragment.equals(WorldFragment.class)) {
            return SCREEN_WORLD;
        }
        return SCREEN_WELCOME;
    }

    public void addPermissionFragments() {

        if (!has(PermissionGrantedFragment.class)) {
            mFragments.add(0, PermissionGrantedFragment.create(mContext, mPager));
            notifyDataSetChanged();
        }
        if (!has(PermissionMissingFragment.class)) {
            mFragments.add(0, PermissionMissingFragment.create(mContext, mPager));
            notifyDataSetChanged();
        }
    }

    public void removePermissionMissingFragment() {
        if (mFragments.get(0) instanceof PermissionMissingFragment) {
            Fragment current = getItem(mPager.getCurrentItem());
            mFragments.remove(0);
            mPager.goTo(current);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if (!(object instanceof Fragment)) {
            throw new RuntimeException("Tried to getItemPosition of non-Fragment");
        }
        int index = mFragments.indexOf(object);
        return index >= 0
                ? index
                : PagerAdapter.POSITION_NONE;
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
        return 3;
    }
}
