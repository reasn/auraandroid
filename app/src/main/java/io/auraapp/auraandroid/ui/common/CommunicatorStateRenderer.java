package io.auraapp.auraandroid.ui.common;


import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;

import static io.auraapp.auraandroid.common.FormattedLog.w;

public class CommunicatorStateRenderer {
    private static final String TAG = "@aura/ui/common/" + CommunicatorStateRenderer.class.getSimpleName();

    public static class InconsistentStateException extends Exception {
        public InconsistentStateException() {
            super("Not scanning or advertising although state indicates that it's possible");
        }
    }

    public static void populateInfoBoxWithState(CommunicatorProxyState proxyState,
                                                InfoBox infoBox,
                                                TextView summary,
                                                Context context) {
        final int NONE = 0;
        final int BOX = 1;
        final int MESSAGE = 2;

        // The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
        int show = MESSAGE;

        Runnable showAuraOffInfoBox = () -> {
            infoBox.setEmoji(":sleeping_sign:");
            infoBox.setHeading(R.string.ui_common_communicator_disabled_heading);
            infoBox.setText(R.string.ui_common_communicator_disabled_text);
            infoBox.hideButton();
            infoBox.setColor(R.color.infoBoxWarning);
        };
        Runnable showGettingReady = () -> {
            summary.setText(EmojiHelper.replaceShortCode(context.getString(R.string.ui_common_communicator_summary_on_not_active)));
            summary.setBackgroundColor(context.getResources().getColor(R.color.yellow));
        };

        @Nullable
        CommunicatorState state = proxyState.mCommunicatorState;

        if (!proxyState.mEnabled) {
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
            infoBox.setHeading(R.string.ui_common_communicator_advertising_not_supported_heading);
            infoBox.setText(R.string.ui_common_communicator_advertising_not_supported_text);
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
