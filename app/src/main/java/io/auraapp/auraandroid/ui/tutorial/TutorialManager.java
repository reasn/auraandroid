package io.auraapp.auraandroid.ui.tutorial;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.ui.ScreenPager;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_COMPLETE_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_OPEN_ACTION;

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
        close();
    }

    public void open() {
        i(TAG, "Opening tutorial and sending intent %s", LOCAL_TUTORIAL_OPEN_ACTION);
        mOpen = true;

        goTo(AuraPrefs.getTutorialStep(mContext));
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

    protected void goTo(Class<? extends TutorialStep> step) {
        goTo(step.getName());
    }

    protected void goTo(String step) {

        close();
        if (step == null) {
            mOpen = false;
            setCompleted(true);
            i(TAG, "Completed tutorial, sending intent %s", LOCAL_TUTORIAL_COMPLETE_ACTION);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(LOCAL_TUTORIAL_COMPLETE_ACTION));
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

        AuraPrefs.putTutorialStep(mContext, mCurrentStep.getClass().getName());

        Button backButton = mCurrentScreen.findViewById(R.id.tutorial_back);
        if (backButton != null) {
            backButton.setOnClickListener($ -> goTo(mCurrentStep.getPrevious()));
        }
        Button nextButton = mCurrentScreen.findViewById(R.id.tutorial_next);
        if (nextButton != null) {
            nextButton.setOnClickListener($ -> {
                Class<? extends TutorialStep> nextStep = mCurrentStep.getNextStep();
                if (nextStep != null) {
                    goTo(nextStep);
                }
            });
        }

        mCurrentScreen.findViewById(R.id.tutorial_overlay).setOnClickListener($ -> {
            // Just catch clicks so that they don't bubble to the background
        });
        mRootView.findViewById(R.id.communicator_state_fragment).setVisibility(View.GONE);
        mRootView.addView(mCurrentScreen);
    }
}
