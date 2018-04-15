package io.auraapp.auraandroid.ui.welcome;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.ActivityState;
import io.auraapp.auraandroid.ui.MainActivity;
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }
        ActivityState state = ((MainActivity) context).getState();
        mPager = state.mPager;
        mTutorialManager = state.mTutorialManager;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        View rootView = inflater.inflate(R.layout.welcome_fragment, container, false);

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
