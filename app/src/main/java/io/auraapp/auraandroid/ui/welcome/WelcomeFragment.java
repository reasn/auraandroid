package io.auraapp.auraandroid.ui.welcome;

import android.content.Context;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;
import io.auraapp.auraandroid.ui.tutorial.EnabledStep;
import io.auraapp.auraandroid.ui.tutorial.TutorialManager;

public class WelcomeFragment extends ScreenFragment implements FragmentCameIntoView {

    private static final String TAG = "@aura/ui/permissions/" + WelcomeFragment.class.getSimpleName();
    private ScreenPager mPager;
    private TutorialManager mTutorialManager;

    @Override
    protected int getLayoutResource() {
        return R.layout.welcome_fragment;
    }

    @Override
    protected void onReady(MainActivity activity, ViewGroup rootView) {

        SharedServicesSet state = activity.getSharedServicesSet();
        mPager = state.mPager;
        mTutorialManager = state.mTutorialManager;

        rootView.findViewById(R.id.welcome_start).setOnClickListener($ -> {
            mPager.goTo(ProfileFragment.class, true);
            mTutorialManager.goTo(EnabledStep.class);
        });

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
