package io.auraapp.auraandroid.ui.welcome;

import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;

public class TermsFragment extends ScreenFragment implements FragmentCameIntoView {

    private static final String TAG = "@aura/ui/welcome/" + TermsFragment.class.getSimpleName();

    @Override
    protected int getLayoutResource() {
        return R.layout.terms_fragment;
    }

    @Override
    protected void onResumeWithContext(MainActivity activity, ViewGroup rootView) {
        rootView.findViewById(R.id.terms_agree).setOnClickListener(
                $ -> activity.getSharedServicesSet().mPager.goTo(WelcomeFragment.class, true));
        rootView.findViewById(R.id.terms_disagree).setOnClickListener($ -> activity.finish());
    }

    @Override
    public void cameIntoView(MainActivity activity) {
        activity.getSharedServicesSet().mPager.setLocked(true);
    }
}
