package io.auraapp.auraandroid.ui.tutorial;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.Set;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.ui.ScreenPager;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_COMPLETED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_OPENED_ACTION;

public class TutorialManager {

    private final static String TAG = "@aura/ui/welcome/" + TutorialManager.class.getSimpleName();

    private final Context mContext;
    private final ViewGroup mRootView;
    private final ScreenPager mPager;
    private View mCurrentScreen;
    private TutorialStep mCurrentStep = null;
    private boolean mOpen;

    public TutorialManager(Context context,
                           ViewGroup tutorialParent,
                           ScreenPager pager) {


        mContext = context;
        mRootView = tutorialParent;
        mPager = pager;
    }

    public void setCompleted(boolean completed) {
        AuraPrefs.putHasCompletedTutorial(mContext, completed);
    }

    public void complete() {
        setCompleted(true);
        goTo((String) null);
    }

    public void open() {
        if (mOpen) {
            return;
        }
        i(TAG, "Opening tutorial and sending intent %s", LOCAL_TUTORIAL_OPENED_ACTION);
        mOpen = true;

        Set<String> completed = AuraPrefs.getCompletedTutorialSteps(mContext);

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(LOCAL_TUTORIAL_OPENED_ACTION));

        if (completed.size() == 0 || completed.contains(FinalStep.class.getName())) {
            // If the user already completed the tutorial, we show first step but allow skipping
            goTo(WelcomeStep.class);
            return;
        }

        // Some tutorial steps (other than WelcomeStep) require the view hierarchy to be built up.
        // Entering them immediately would lead to crashes
        // Waiting 500ms is a very dirty solution for the problem

        new Handler().postDelayed(() -> {

            if (completed.contains(WorldStep.class.getName())) {
                goTo(FinalStep.class);
            } else if (completed.contains(SwipeStep.class.getName())) {
                goTo(WorldStep.class);
            } else if (completed.contains(SloganAddStep.class.getName())) {
                goTo(SwipeStep.class);
            } else if (completed.contains(TextStep.class.getName())) {
                goTo(SloganAddStep.class);
            } else if (completed.contains(NameStep.class.getName())) {
                goTo(TextStep.class);
            } else if (completed.contains(ColorStep.class.getName())) {
                goTo(NameStep.class);
            } else if (completed.contains(EnabledStep.class.getName())) {
                goTo(ColorStep.class);
            } else if (completed.contains(WelcomeStep.class.getName())) {
                goTo(EnabledStep.class);
            } else {
                goTo(WelcomeStep.class);
            }
        }, 500);
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

    protected void completeCurrentAndGoTo(Class<? extends TutorialStep> step) {

        if (mCurrentStep != null) {
            AuraPrefs.markTutorialStepAsCompleted(mContext, mCurrentStep.getClass().getName());
        }
        goTo(step);
    }

    protected void goTo(Class<? extends TutorialStep> step) {
        goTo(step == null ? null : step.getName());
    }

    protected void goTo(String step) {

        close();

        if (step == null) {
            mOpen = false;
            setCompleted(true);
            i(TAG, "Completed tutorial, sending intent %s", LOCAL_TUTORIAL_COMPLETED_ACTION);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(LOCAL_TUTORIAL_COMPLETED_ACTION));
            mRootView.findViewById(R.id.communicator_state_fragment).setVisibility(View.VISIBLE);
            return;
        }

        if (step.equals(EnabledStep.class.getName())) {
            mCurrentStep = new EnabledStep(mRootView, mContext, mPager, this);
        } else if (step.equals(SwipeStep.class.getName())) {
            mCurrentStep = new SwipeStep(mRootView, mContext, mPager, this);
        } else if (step.equals(ColorStep.class.getName())) {
            mCurrentStep = new ColorStep(mRootView, mContext, mPager, this);
        } else if (step.equals(NameStep.class.getName())) {
            mCurrentStep = new NameStep(mRootView, mContext, mPager, this);
        } else if (step.equals(TextStep.class.getName())) {
            mCurrentStep = new TextStep(mRootView, mContext, mPager, this);
        } else if (step.equals(SloganAddStep.class.getName())) {
            mCurrentStep = new SloganAddStep(mRootView, mContext, mPager, this);
        } else if (step.equals(WorldStep.class.getName())) {
            mCurrentStep = new WorldStep(mRootView, mContext, mPager, this);
        } else if (step.equals(FinalStep.class.getName())) {
            mCurrentStep = new FinalStep(mRootView, mContext, mPager, this);
        } else {
            mCurrentStep = new WelcomeStep(mRootView, mContext, mPager, this);

        }
        mCurrentScreen = mCurrentStep.enter();

        Button backButton = mCurrentScreen.findViewById(R.id.tutorial_back);
        if (backButton != null) {
            backButton.setOnClickListener($ -> goTo(mCurrentStep.getPrevious()));
        }
        Button nextButton = mCurrentScreen.findViewById(R.id.tutorial_next);
        if (nextButton != null) {
            if (AuraPrefs.getCompletedTutorialSteps(mContext).contains(mCurrentStep.getClass().getName())) {
                //Allow the user to progress if they completed this step before
                nextButton.setVisibility(View.VISIBLE);
            }

            nextButton.setOnClickListener($ -> completeCurrentAndGoTo(mCurrentStep.getNextStep()));
        }

        mCurrentScreen.findViewById(R.id.tutorial_overlay).setOnClickListener($ -> {
            // Just catch clicks so that they don't bubble to the background
        });
        mRootView.findViewById(R.id.communicator_state_fragment).setVisibility(View.GONE);
        mRootView.addView(mCurrentScreen);
    }
}
