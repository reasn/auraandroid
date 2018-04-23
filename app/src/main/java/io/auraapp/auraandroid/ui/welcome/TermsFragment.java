package io.auraapp.auraandroid.ui.welcome;

import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

import static android.content.Context.MODE_PRIVATE;

public class TermsFragment extends ScreenFragment implements FragmentCameIntoView {

    private static final String TAG = "@aura/ui/welcome/" + TermsFragment.class.getSimpleName();

    @Override
    protected int getLayoutResource() {
        return R.layout.terms_fragment;
    }

    @Override
    protected void onResumeWithContext(MainActivity activity, ViewGroup rootView) {
        rootView.findViewById(R.id.terms_agree).setOnClickListener(
                $ -> {
                    ScreenPager pager = activity.getSharedServicesSet().mPager;
                    activity.getSharedPreferences(Config.PREFERENCES_BUCKET, MODE_PRIVATE)
                            .edit()
                            .putBoolean(getString(R.string.prefs_terms_agreed), true)
                            .apply();
                    if (!pager.redirectIfNeeded(activity, null)) {
                        pager.setSwipeLocked(false);
                        pager.goTo(ProfileFragment.class, true);
                    }
                });
        rootView.findViewById(R.id.terms_disagree).setOnClickListener($ -> activity.finish());
    }

    @Override
    public void cameIntoView(MainActivity activity) {
        activity.getSharedServicesSet().mPager.setSwipeLocked(true);
    }
}
