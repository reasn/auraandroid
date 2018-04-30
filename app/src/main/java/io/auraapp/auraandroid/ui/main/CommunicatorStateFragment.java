package io.auraapp.auraandroid.ui.main;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.fragments.ContextViewFragment;

import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_SCREEN_PAGER_CHANGED_ACTION;

public class CommunicatorStateFragment extends ContextViewFragment {

    private static final String TAG = "@aura/ui/" + CommunicatorStateFragment.class.getSimpleName();
    public static final int BLUETOOTH_ENABLE_REQUEST_ID = 6543;
    private CommunicatorProxyState mCommunicatorProxyState;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context $, Intent intent) {
            v(TAG, "onReceive, intent: %s", intent.getAction());
            Bundle extras = intent.getExtras();

            if (extras != null && LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                mCommunicatorProxyState = (CommunicatorProxyState) extras.getSerializable(IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE);
            }
            //Reflect state for all registered actions (communicator state & screen pager)
            reflectCommunicatorState();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.main_communicator_state_fragment;
    }

    @Override
    protected void onResumeWithContextAndView(MainActivity activity, ViewGroup rootView) {
        LocalBroadcastManager.getInstance(activity).registerReceiver(mReceiver, IntentFactory.createFilter(
                LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION,
                LOCAL_SCREEN_PAGER_CHANGED_ACTION
        ));
        v(TAG, "Receiver registered");
        mCommunicatorProxyState = activity.getSharedServicesSet().mCommunicatorProxy.getState();
        reflectCommunicatorState();
    }

    private void reflectCommunicatorState() {
        // getContext() was observed to be null after long inactivity of the app
        if (getRootView() != null && getContext() != null) {
            populateInfoBoxWithState(getRootView().findViewById(R.id.communicator_state_info_box),
                    getRootView().findViewById(R.id.communicator_state_summary),
                    getContext());
        }
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mReceiver);
        v(TAG, "Receiver unregistered");
    }

    private void populateInfoBoxWithState(InfoBox infoBox,
                                          TextView summary,
                                          Context context) {
        final int NONE = 0;
        final int BOX = 1;
        final int MESSAGE = 2;

        // The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
        int show = MESSAGE;

        Runnable showAuraOffInfoBox = () -> {
            infoBox.setEmoji("❗️");
            infoBox.setHeading(R.string.common_communicator_disabled_heading);
            infoBox.setText(R.string.common_communicator_disabled_text);
            infoBox.hideButton();
            infoBox.setColor(R.color.infoBoxWarning);
        };
        Runnable showGettingReady = () -> {
            summary.setText(EmojiHelper.replaceShortCode(context.getString(R.string.ui_common_communicator_summary_on_not_active)));
            summary.setBackgroundColor(context.getResources().getColor(R.color.yellow));
        };

        @Nullable
        CommunicatorState state = mCommunicatorProxyState.mCommunicatorState;
        infoBox.setOnClickListener(null);

        if (!PermissionHelper.granted(context) || !AuraPrefs.hasAgreedToTerms(context)) {
            // In this case user is in PermissionFragment and needs no additional summary/box
            show = NONE;

        } else if (!mCommunicatorProxyState.mEnabled) {
            showAuraOffInfoBox.run();
            show = BOX;
        } else if (state == null) {
            showGettingReady.run();

        } else if (state.mBluetoothRestartRequired) {
            infoBox.setEmoji(":dizzy_face:");
            infoBox.setHeading(R.string.ui_common_communicator_bt_restart_required_heading);
            infoBox.setText(context.getString(R.string.ui_common_communicator_bt_restart_required_text)
                    .replaceAll("##error##", state.mLastError != null ? state.mLastError : "unknown"));
            infoBox.hideButton();
            infoBox.setColor(R.color.infoBoxError);
            show = BOX;

        } else if (state.mBtTurningOn) {
            summary.setText(EmojiHelper.replaceShortCode(context.getString(R.string.ui_common_communicator_summary_bt_turning_on)));
            summary.setBackgroundColor(context.getResources().getColor(R.color.yellow));
        } else if (!state.mBtEnabled) {
            infoBox.setEmoji(":broken_heart:");
            infoBox.setHeading(R.string.ui_common_communicator_bt_disabled_heading);
            infoBox.setText(R.string.ui_common_communicator_bt_disabled_text);
            infoBox.hideButton();
            infoBox.setColor(R.color.infoBoxWarning);
            infoBox.setOnClickListener($ -> startActivityForResult(
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    BLUETOOTH_ENABLE_REQUEST_ID));
            show = BOX;
        } else if (!state.mBleSupported) {
            infoBox.setEmoji(":dizzy_face:");
            infoBox.setHeading(context.getString(R.string.ui_common_communicator_ble_not_supported_heading));
            infoBox.setText(R.string.ui_common_communicator_ble_not_supported_text);
            infoBox.hideButton();
            infoBox.setColor(R.color.infoBoxError);
            show = BOX;
        } else if (!state.mShouldCommunicate) {
            showAuraOffInfoBox.run();
            show = BOX;
        } else if (!state.mAdvertisingSupported) {
            infoBox.setEmoji(":broken_heart:");
            infoBox.setHeading(R.string.common_communicator_advertising_not_supported_heading);
            infoBox.setText(R.string.common_communicator_advertising_not_supported_text);
            infoBox.hideButton();
            infoBox.setColor(R.color.infoBoxWarning);
            show = BOX;
        } else if (!state.mAdvertising) {
            w(TAG, "Not advertising although it is possible.");
            showGettingReady.run();
        } else if (!state.mScanning) {
            w(TAG, "Not scanning although it is possible.");
            showGettingReady.run();

        } else {
            show = NONE;
        }
        // The following LayoutParams magic is necessary to hide the list item entirely if show == BOX
        // because setting the item's visibility to GONE doesn't do the job.
        // Thanks to https://stackoverflow.com/questions/41223413/how-to-hide-an-item-from-recycler-view-on-a-particular-condition
        if (show == BOX) {
            summary.setVisibility(View.GONE);
            infoBox.setVisibility(View.VISIBLE);
        } else if (show == MESSAGE) {
            summary.setVisibility(View.VISIBLE);
            summary.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            infoBox.setVisibility(View.GONE);
        } else {
            infoBox.setVisibility(View.GONE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.height = 0;
            summary.setLayoutParams(params);
        }
    }
}
