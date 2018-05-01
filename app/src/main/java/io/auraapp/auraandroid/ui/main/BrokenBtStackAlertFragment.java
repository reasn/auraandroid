package io.auraapp.auraandroid.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.ui.DialogManager;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.common.fragments.ContextFragment;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;

public class BrokenBtStackAlertFragment extends ContextFragment {
    private static final String TAG = "@aura/ui/main/" + BrokenBtStackAlertFragment.class.getSimpleName();
    private static final int BROKEN_BT_STACK_ALERT_DEBOUNCE = 1000 * 60;
    private boolean mInForeground;
    private long mLastVisibleTimestamp = 0;

    @Override
    protected void onResumeWithContext(MainActivity activity) {
        v(TAG, "onResumeWithContext");
        DialogManager dialogManager = activity.getSharedServicesSet().mDialogManager;

        LocalBroadcastManager.getInstance(activity).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context $, Intent intent) {
                v(TAG, "onReceive, intent: %s", intent.getAction());
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CommunicatorProxyState proxyState = (CommunicatorProxyState) extras.getSerializable(IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE);
                d(TAG, "Received communicator proxy state, mInForeground: %s, state: %s",
                        mInForeground ? "true" : "false",
                        proxyState);

                if (!mInForeground) {
                    return;
                }

                if (proxyState == null || proxyState.mCommunicatorState == null) {
                    return;
                }
                if (proxyState.mCommunicatorState.mRecentBtTurnOnEvents < Config.COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD) {
                    return;
                }
                if (mLastVisibleTimestamp > 0
                        && System.currentTimeMillis() - mLastVisibleTimestamp <= BROKEN_BT_STACK_ALERT_DEBOUNCE) {
                    return;
                }
                if (AuraPrefs.shouldHideBrokenBtStackAlert(activity)) {
                    i(TAG, "Not showing alert");
                    return;
                }
                i(TAG, "Showing alert");
                dialogManager.showBtBroken(neverShowAgain -> {
                    if (neverShowAgain) {
                        AuraPrefs.putHideBrokenBtStackAlert(activity);
                    } else {
                        mLastVisibleTimestamp = System.currentTimeMillis();
                    }
                });
            }
        }, IntentFactory.createFilter(LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION));

        mInForeground = true;
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        mInForeground = false;
    }
}
