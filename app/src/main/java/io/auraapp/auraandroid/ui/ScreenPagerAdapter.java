package io.auraapp.auraandroid.ui;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.ui.debug.DebugFragment;
import io.auraapp.auraandroid.ui.permissions.PermissionsFragment;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;
import io.auraapp.auraandroid.ui.welcome.TermsFragment;
import io.auraapp.auraandroid.ui.world.WorldFragment;

import static io.auraapp.auraandroid.common.FormattedLog.quickDump;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_EXTRA_NEW;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_EXTRA_PREVIOUS;

public class ScreenPagerAdapter extends FragmentPagerAdapter {
    public static final String SCREEN_PRIVACY = "privacy";
    private static final String SCREEN_WORLD = "world";
    private static final String SCREEN_PROFILE = "profile";
    private final List<Fragment> mFragments = new ArrayList<>();
    private final LocalBroadcastManager mLocalBroadcastManager;

    public ScreenPagerAdapter(FragmentManager fm, LocalBroadcastManager localBroadcastManager) {
        super(fm);
        mLocalBroadcastManager = localBroadcastManager;
        mFragments.add(new ProfileFragment());
        mFragments.add(new WorldFragment());
    }

    private boolean has(Class fragmentClass) {
        return find(fragmentClass) >= 0;
    }

    private int find(Class fragmentClass) {
        for (int i = 0; i < mFragments.size(); i++) {
            if (mFragments.get(i).getClass().equals(fragmentClass)) {
                return i;
            }
        }
        return -1;
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
        if (!has(TermsFragment.class)) {
            mFragments.add(find(ProfileFragment.class), new TermsFragment());
            notifyDataSetChanged();
        }
    }

    public void remove(Class<?> fragmentClass) {
        for (Fragment candidate : mFragments.toArray(new Fragment[mFragments.size()])) {
            if (candidate.getClass().equals(fragmentClass)) {
                mFragments.remove(candidate);
                notifyDataSetChanged();
                return;
            }
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
    }

    private Fragment mPrimaryItem = null;

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);

        if (object == mPrimaryItem) {
            return;
        }

        Intent intent = new Intent(LOCAL_SCREEN_PAGER_CHANGED_ACTION);
        intent.putExtra(LOCAL_SCREEN_PAGER_CHANGED_EXTRA_NEW, object.getClass().toString());
        intent.putExtra(LOCAL_SCREEN_PAGER_CHANGED_EXTRA_PREVIOUS, mPrimaryItem == null
                ? "null"
                : mPrimaryItem.getClass().toString());
        quickDump("Sending broadcast " + object.getClass().toString());
        mLocalBroadcastManager.sendBroadcast(intent);

        mPrimaryItem = (Fragment) object;
    }

    @Override
    public long getItemId(int position) {
        Fragment fragment = mFragments.get(position);
        if (fragment == null) {
            throw new Error("Tried to get ID of null fragment at " + position);
        }
        if (fragment instanceof PermissionsFragment) {
            return PermissionsFragment.FRAGMENT_ID;
        }
        if (fragment instanceof TermsFragment) {
            return TermsFragment.FRAGMENT_ID;
        }
        if (mFragments.get(position) instanceof ProfileFragment) {
            return ProfileFragment.FRAGMENT_ID;
        }
        if (mFragments.get(position) instanceof WorldFragment) {
            return WorldFragment.FRAGMENT_ID;
        }
        if (mFragments.get(position) instanceof DebugFragment) {
            return DebugFragment.FRAGMENT_ID;
        }
        throw new Error("Tried to get ID of unknown fragment " + fragment.getClass().getSimpleName());
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if (!(object instanceof Fragment)) {
            throw new RuntimeException("Tried to getItemPosition of non-Fragment");
        }
//        return PagerAdapter.POSITION_NONE;
        // TODO don't always return POSITION_NONE. Problem seems to come from changing item positions
        // Didn't work, sometimes fragments where added twice, app crashed after agreeing to terms
        int index = mFragments.indexOf(object);
        quickDump(object.getClass().getSimpleName() + ": " + index);
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
        return mFragments == null ? 0 : mFragments.size();
    }
}
