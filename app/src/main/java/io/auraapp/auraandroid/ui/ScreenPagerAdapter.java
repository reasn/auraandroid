package io.auraapp.auraandroid.ui;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_EXTRA_NEW;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_EXTRA_PREVIOUS;

public class ScreenPagerAdapter extends FragmentPagerAdapter {
    public static final long ID_PERMISSIONS = 5032;
    public static final long ID_TERMS = 5033;
    public static final long ID_PROFILE = 5034;
    public static final long ID_WORLD = 5035;
    public static final long ID_DEBUG = 5036;
    private static final String TAG = "@aura/ui/" + ScreenPagerAdapter.class.getSimpleName();
    private final List<Fragment> mFragments = new ArrayList<>();
    private final LocalBroadcastManager mLocalBroadcastManager;

    private Fragment mCurrentItem = null;

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

    @Nullable
    public Fragment getCurrentItem() {
        return mCurrentItem;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        sendChangeBroadcast((Fragment) object);
    }

    public void sendChangeBroadcast(Fragment fragment) {
        if (mCurrentItem != null && fragment.getClass().equals(mCurrentItem.getClass())) {
            return;
        }
        String previous = mCurrentItem == null
                ? "null"
                : mCurrentItem.getClass().toString();
        i(TAG, "Broadcasting new primary fragment %s, was: %s", fragment.getClass().getSimpleName(), previous);
        Intent intent = new Intent(LOCAL_SCREEN_PAGER_CHANGED_ACTION);
        intent.putExtra(LOCAL_SCREEN_PAGER_CHANGED_EXTRA_NEW, fragment.getClass().toString());
        intent.putExtra(LOCAL_SCREEN_PAGER_CHANGED_EXTRA_PREVIOUS, previous);
        mLocalBroadcastManager.sendBroadcast(intent);

        mCurrentItem = fragment;
    }

    @Override
    public long getItemId(int position) {
        Fragment fragment = mFragments.get(position);
        if (fragment == null) {
            throw new Error("Tried to get ID of null fragment at " + position);
        }
        if (fragment instanceof PermissionsFragment) {
            return ID_PERMISSIONS;
        }
        if (fragment instanceof TermsFragment) {
            return ID_TERMS;
        }
        if (mFragments.get(position) instanceof ProfileFragment) {
            return ID_PROFILE;
        }
        if (mFragments.get(position) instanceof WorldFragment) {
            return ID_WORLD;
        }
        if (mFragments.get(position) instanceof DebugFragment) {
            return ID_DEBUG;
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
