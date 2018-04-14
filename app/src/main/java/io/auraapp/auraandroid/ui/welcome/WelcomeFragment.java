package io.auraapp.auraandroid.ui.welcome;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;
import io.auraapp.auraandroid.ui.tutorial.EnabledStep;
import io.auraapp.auraandroid.ui.tutorial.TutorialManager;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class WelcomeFragment extends ScreenFragment implements FragmentCameIntoView {

    private static final String TAG = "@aura/ui/permissions/" + WelcomeFragment.class.getSimpleName();
    private ScreenPager mPager;
    private TutorialManager mTutorialManager;

    public static WelcomeFragment create(Context context, ScreenPager pager, TutorialManager tutorialManager) {
        WelcomeFragment fragment = new WelcomeFragment();
        fragment.setContext(context);
        fragment.mPager = pager;
        fragment.mTutorialManager = tutorialManager;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.welcome_fragment, container, false);

        rootView.findViewById(R.id.welcome_start).setOnClickListener($ -> {
            mPager.goTo(ProfileFragment.class, true);
            mTutorialManager.goTo(EnabledStep.class);
        });

        return rootView;
    }

    @Override
    public void cameIntoView() {
        mPager.setLocked(true);
    }
}
