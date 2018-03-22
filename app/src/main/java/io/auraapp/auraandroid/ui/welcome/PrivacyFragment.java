package io.auraapp.auraandroid.ui.welcome;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PrivacyFragment extends Fragment {

    private static final String TAG = "@aura/ui/permissions/" + PrivacyFragment.class.getSimpleName();
    private Context mContext;
    private ScreenPager mPager;

    public static PrivacyFragment create(Context context, ScreenPager pager) {
        PrivacyFragment fragment = new PrivacyFragment();
        fragment.mContext = context;
        fragment.mPager = pager;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_screen_privacy, container, false);

        return rootView;
    }
}
