package io.auraapp.auraandroid.ui.tutorial;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.ui.ScreenPager;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_COMPLETE_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_OPEN_ACTION;

public class TutorialManager {

    private static String TAG = "@aura/ui/welcome/" + TutorialManager.class.getSimpleName();

    private final Context mContext;
    private final RelativeLayout mRootView;
    private final ScreenPager mPager;
    private View mCurrentScreen;
    private TutorialStep mCurrentStep = null;
    private boolean mOpen;

    public TutorialManager(Context context,
                           RelativeLayout rootView,
                           ScreenPager pager) {
        mContext = context;
        mRootView = rootView;
        mPager = pager;
    }

    // TODO invert tutorial colors if aura is dark

    public void setCompleted(boolean completed) {
        AuraPrefs.putHasCompletedTutorial(mContext, completed);
    }

    public void complete() {
        setCompleted(true);
        close();
    }

    public void open() {
        i(TAG, "Opening tutorial and sending intent %s", LOCAL_TUTORIAL_OPEN_ACTION);
        mOpen = true;
        goTo(WelcomeStep.class);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(LOCAL_TUTORIAL_OPEN_ACTION));
    }

    public void close() {
        i(TAG, "Closing tutorial");
        if (mCurrentStep != null) {
            i(TAG, "Removing current tutorial screen");
            ((ViewGroup) mCurrentScreen.getParent()).removeView(mCurrentScreen);
            mCurrentStep.leave();
            mCurrentScreen = null;
            mCurrentStep = null;
        }
    }

    public boolean isOpen() {
        return mOpen;
    }

    private void goTo(Class<? extends TutorialStep> step) {

        close();
        if (step == null) {
            mOpen = false;
            setCompleted(true);
            i(TAG, "Completed tutorial, sending intent %s", LOCAL_TUTORIAL_COMPLETE_ACTION);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(LOCAL_TUTORIAL_COMPLETE_ACTION));
            mRootView.findViewById(R.id.communicator_state_fragment).setVisibility(View.VISIBLE);
            return;
        }
        if (step.equals(WelcomeStep.class)) {
            mCurrentStep = new WelcomeStep(mRootView, mContext, mPager);
        } else if (step.equals(EnabledStep.class)) {
            mCurrentStep = new EnabledStep(mRootView, mContext, mPager);
        } else if (step.equals(SwipeStep.class)) {
            mCurrentStep = new SwipeStep(mRootView, mContext, mPager);
        } else if (step.equals(ColorStep.class)) {
            mCurrentStep = new ColorStep(mRootView, mContext, mPager);
        } else if (step.equals(NameStep.class)) {
            mCurrentStep = new NameStep(mRootView, mContext, mPager);
        } else if (step.equals(TextStep.class)) {
            mCurrentStep = new TextStep(mRootView, mContext, mPager);
        } else if (step.equals(SloganAddStep.class)) {
            mCurrentStep = new SloganAddStep(mRootView, mContext, mPager);
        } else if (step.equals(AdoptStep.class)) {
            mCurrentStep = new AdoptStep(mRootView, mContext, mPager);
        } else if (step.equals(FinalStep.class)) {
            mCurrentStep = new FinalStep(mRootView, mContext, mPager);
        }
        mCurrentScreen = mCurrentStep.enter();

        Button backButton = mCurrentScreen.findViewById(R.id.tutorial_back);
        if (backButton != null) {
            backButton.setOnClickListener($ -> goTo(mCurrentStep.getPrevious()));
        }
        Button nextButton = mCurrentScreen.findViewById(R.id.tutorial_next);
        if (nextButton != null) {
            nextButton.setOnClickListener($ -> goTo(mCurrentStep.getNextStep()));
        }

        mCurrentScreen.findViewById(R.id.tutorial_overlay).setOnClickListener($ -> {
            // Just catch clicks so that they don't bubble to the background
        });
        mRootView.findViewById(R.id.communicator_state_fragment).setVisibility(View.GONE);
        mRootView.addView(mCurrentScreen);
    }
}
