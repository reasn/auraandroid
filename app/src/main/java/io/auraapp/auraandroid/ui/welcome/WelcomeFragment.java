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

public class WelcomeFragment extends Fragment {

    private static final String TAG = "@aura/ui/permissions/" + WelcomeFragment.class.getSimpleName();
    private Context mContext;
    private ScreenPager mPager;

    public static WelcomeFragment create(Context context, ScreenPager pager) {
        WelcomeFragment fragment = new WelcomeFragment();
        fragment.mContext = context;
        fragment.mPager = pager;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.welcome_fragment, container, false);

        return rootView;
    }
}
