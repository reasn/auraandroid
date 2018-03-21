package io.auraapp.auraandroid.tutorial;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;

public class ProfileFragment extends Fragment implements FragmentWithToolbarButtons {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_screen_profile, container, false);

        return rootView;
    }
}
