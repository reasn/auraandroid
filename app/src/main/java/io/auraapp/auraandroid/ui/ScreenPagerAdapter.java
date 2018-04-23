package io.auraapp.auraandroid.ui;

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
import io.auraapp.auraandroid.ui.welcome.TermsFragment;
import io.auraapp.auraandroid.ui.world.WorldFragment;

public class ScreenPagerAdapter extends FragmentStatePagerAdapter {
    public static final String SCREEN_PRIVACY = "privacy";
    private static final String SCREEN_WORLD = "world";
    private static final String SCREEN_PROFILE = "profile";
    private final List<Fragment> mFragments;

    public ScreenPagerAdapter(FragmentManager fm) {
        super(fm);
        mFragments = new ArrayList<>();
        mFragments.add(new ProfileFragment());
        mFragments.add(new WorldFragment());
    }

    private boolean has(Class fragmentClass) {
        for (Fragment fragment : mFragments) {
            if (fragment.getClass().equals(fragmentClass)) {
                return true;
            }
        }
        return false;
    }

    public boolean debugVisible() {
        return has(DebugFragment.class);
    }

    public Class getClassForHandle(String handle) {
        if (handle.equals(SCREEN_PROFILE)) {
            return ProfileFragment.class;
        }
        if (handle.equals(SCREEN_WORLD)) {
            return WorldFragment.class;
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
        return SCREEN_PRIVACY;
    }

    public void addPermissionsFragment() {
        if (!has(PermissionsFragment.class)) {
            mFragments.add(0, new PermissionsFragment());
            notifyDataSetChanged();
        }
    }

    public void addDebugFragment() {
        if (!has(DebugFragment.class)) {
            mFragments.add(new DebugFragment());
            notifyDataSetChanged();
        }
    }

    public void addTermsFragment() {
        boolean added = false;
        if (!has(TermsFragment.class)) {
            mFragments.add(0, new TermsFragment());
            added = true;
        }
        if (added) {
            notifyDataSetChanged();
        }
    }

    public void removeTermsFragment() {
        boolean removed = false;
        for (Fragment fragment : mFragments.toArray(new Fragment[mFragments.size()])) {
            if (fragment instanceof TermsFragment) {
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
        // Would affect swipes only and all screens' views are inflated continuously anyway
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
