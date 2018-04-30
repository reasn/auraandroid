package io.auraapp.auraandroid.ui.welcome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.fragments.ContextViewFragment;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;

import static io.auraapp.auraandroid.common.FormattedLog.quickDump;

public class TermsFragment extends ContextViewFragment {

    private static final String TAG = "@aura/ui/welcome/" + TermsFragment.class.getSimpleName();
    public static final long FRAGMENT_ID = 5033;
    private ScreenPager mPager;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            quickDump(intent);
            String newFragmentClass = intent.getStringExtra(IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_EXTRA_NEW);
            if (TermsFragment.class.toString().equals(newFragmentClass)) {
                mPager.setSwipeLocked(true);
            }

            String previousFragmentClass = intent.getStringExtra(IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_EXTRA_PREVIOUS);
            if (mPager != null && TermsFragment.class.toString().equals(previousFragmentClass)) {
                mPager.getScreenAdapter().remove(TermsFragment.class);
            }
            quickDump(previousFragmentClass);
            quickDump(newFragmentClass);
        }
    };

    @Override
    protected int getLayoutResource() {
        return R.layout.terms_fragment;
    }

    @Override
    protected void onResumeWithContextAndView(MainActivity activity, ViewGroup rootView) {
        mPager = activity.getSharedServicesSet().mPager;
        rootView.findViewById(R.id.terms_agree).setOnClickListener(
                $ -> {
                    ScreenPager pager = activity.getSharedServicesSet().mPager;
                    AuraPrefs.putHasAgreedToTerms(activity, true);
                    if (!pager.redirectIfNeeded(activity, null)) {
                        pager.setSwipeLocked(false);
                        pager.goTo(ProfileFragment.class, true);
                    }
                });
        rootView.findViewById(R.id.terms_disagree).setOnClickListener($ -> activity.finish());
        LocalBroadcastManager.getInstance(activity).registerReceiver(
                mReceiver,
                IntentFactory.createFilter(IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_ACTION)
        );
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mReceiver);
    }
}
