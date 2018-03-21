package io.auraapp.auraandroid.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.ui.permissions.PermissionGrantedFragment;
import io.auraapp.auraandroid.ui.permissions.PermissionMissingFragment;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;
import io.auraapp.auraandroid.ui.welcome.WelcomeFragment;
import io.auraapp.auraandroid.ui.world.WorldFragment;

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
        mFragments.add(WelcomeFragment.create(context, pager));
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
            mFragments.remove(0);
            notifyDataSetChanged();
            mPager.goTo(PermissionGrantedFragment.class, false);
        }
    }

    public void removePermissionGrantedFragment() {
        if (mFragments.get(0) instanceof PermissionGrantedFragment) {
            mFragments.remove(0);
            notifyDataSetChanged();
            mPager.goTo(WelcomeFragment.class, false);
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if (!(object instanceof Fragment)) {
            throw new RuntimeException("Tried to getItemPosition of non-Fragment");
        }
        return PagerAdapter.POSITION_NONE;
        // Didn't work:
        //        int index = mFragments.indexOf(object);
        //        return index >= 0
        //                ? index
        //                : PagerAdapter.POSITION_NONE;
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
