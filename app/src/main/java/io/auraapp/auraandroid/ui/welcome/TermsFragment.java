package io.auraapp.auraandroid.ui.welcome;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class TermsFragment extends ScreenFragment implements FragmentCameIntoView {

    private static final String TAG = "@aura/ui/welcome/" + TermsFragment.class.getSimpleName();
    private ScreenPager mPager;
    private Activity mActivity;

    public static TermsFragment create(Activity activity, ScreenPager pager) {
        TermsFragment fragment = new TermsFragment();
        fragment.setContext(activity);
        fragment.mPager = pager;
        fragment.mActivity = activity;
        return fragment;
    }

    @Override
    @ExternalInvocation
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.terms_fragment, container, false);

        rootView.findViewById(R.id.terms_agree).setOnClickListener($ -> mPager.goTo(WelcomeFragment.class, true));
        rootView.findViewById(R.id.terms_disagree).setOnClickListener($ -> mActivity.finish());

        return rootView;
    }

    @Override
    public void cameIntoView() {
        mPager.setLocked(true);
    }
}
