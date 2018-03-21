package io.auraapp.auraandroid.tutorial;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WorldFragment extends Fragment implements FragmentWithToolbarButtons {

    private ViewGroup mRootView;

    public static WorldFragment create(ViewGroup worldView) {
        WorldFragment fragment = new WorldFragment();
        fragment.mRootView = worldView;
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mRootView;
    }
}
