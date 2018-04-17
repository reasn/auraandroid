package io.auraapp.auraandroid.ui.welcome;

import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;
import io.auraapp.auraandroid.ui.tutorial.EnabledStep;

public class WelcomeFragment extends ScreenFragment implements FragmentCameIntoView {

    @Override
    protected int getLayoutResource() {
        return R.layout.welcome_fragment;
    }

    @Override
    protected void onResumeWithContext(MainActivity activity, ViewGroup rootView) {

        rootView.findViewById(R.id.welcome_start).setOnClickListener($ -> {
            SharedServicesSet servicesSet = activity.getSharedServicesSet();
            servicesSet.mPager.goTo(ProfileFragment.class, true);
            servicesSet.mTutorialManager.goTo(EnabledStep.class);
        });
    }

    @Override
    public void cameIntoView(MainActivity activity) {
        activity.getSharedServicesSet().mPager.setLocked(true);
    }
}
