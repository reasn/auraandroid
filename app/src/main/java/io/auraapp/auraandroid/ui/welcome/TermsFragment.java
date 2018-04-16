package io.auraapp.auraandroid.ui.welcome;

import android.content.Context;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;

public class TermsFragment extends ScreenFragment implements FragmentCameIntoView {

    private static final String TAG = "@aura/ui/welcome/" + TermsFragment.class.getSimpleName();
    private ScreenPager mPager;

    @Override
    protected int getLayoutResource() {
        return R.layout.terms_fragment;
    }

    @Override
    protected void onReady(MainActivity activity, ViewGroup rootView) {
        mPager = activity.getSharedServicesSet().mPager;

        rootView.findViewById(R.id.terms_agree).setOnClickListener($ -> mPager.goTo(WelcomeFragment.class, true));
        rootView.findViewById(R.id.terms_disagree).setOnClickListener($ -> activity.finish());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }
    }

    @Override
    public void cameIntoView() {
        mPager.setLocked(true);
    }
}
