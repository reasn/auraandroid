package io.auraapp.auraandroid.ui.welcome;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.ScreenPager;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class TermsFragment extends ScreenFragment implements FragmentCameIntoView {

    private static final String TAG = "@aura/ui/welcome/" + TermsFragment.class.getSimpleName();
    private ScreenPager mPager;
    private View.OnClickListener finishActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }
        mPager = ((MainActivity) context).getSharedServicesSet().mPager;
        finishActivity = $ -> ((MainActivity) context).finish();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.terms_fragment, container, false);

        rootView.findViewById(R.id.terms_agree).setOnClickListener($ -> mPager.goTo(WelcomeFragment.class, true));
        rootView.findViewById(R.id.terms_disagree).setOnClickListener(finishActivity);

        return rootView;
    }

    @Override
    public void cameIntoView() {
        mPager.setLocked(true);
    }
}
