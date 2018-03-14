package io.auraapp.auraandroid.main.list.item;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.main.InfoBox;
import io.auraapp.auraandroid.main.PeerSlogan;

import static io.auraapp.auraandroid.common.FormattedLog.w;

public class StatusHolder extends ItemViewHolder {

    private final InfoBox mInfoBox;
    private static final String TAG = "aura/main/list/item/" + StatusHolder.class.getSimpleName();
    private final TextView mSummaryTextView;
    private final Context mContext;

    public StatusHolder(Context context, View itemView) {
        super(itemView);
        mContext = context;
        mSummaryTextView = itemView.findViewById(R.id.status_summary);
        mInfoBox = itemView.findViewById(R.id.info_box);
    }

    private void showAuraOffInfoBox() {
        mInfoBox.setEmoji(":sleeping_sign:");
        mInfoBox.setHeading(R.string.ui_main_status_communicator_disabled_heading);
        mInfoBox.setText(R.string.ui_main_status_communicator_disabled_text);
        mInfoBox.hideButton();
        mInfoBox.setColor(R.color.infoBoxWarning);
    }

    public void bind(ListItem item, View itemView) {
        if (!(item instanceof StatusItem)) {
            throw new RuntimeException("Expecting " + StatusItem.class.getSimpleName());
        }
        StatusItem castItem = (StatusItem) item;
        @Nullable CommunicatorState state = castItem.mState;
        TreeMap<String, PeerSlogan> peerSloganMap = castItem.mPeerSloganMap;
        Set<Peer> peers = castItem.mPeers;
        if (peerSloganMap == null || peers == null) {
            return;
        }

        // The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
        boolean showInfoBox = false;
        if (state == null) {
            showAuraOffInfoBox();
            showInfoBox = true;


        } else if (state.mBluetoothRestartRequired) {
            mInfoBox.setEmoji(":dizzy_face:");
            mInfoBox.setHeading(R.string.ui_main_status_communicator_bt_restart_required_heading);
            mInfoBox.setText(mContext.getString(R.string.ui_main_status_communicator_bt_restart_required_text)
                    .replaceAll("##error##", state.mLastError != null ? state.mLastError : "unknown"));
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxError);
            showInfoBox = true;

        } else if (state.mBtTurningOn) {
            mSummaryTextView.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_status_summary_communicator_bt_turning_on)));
            mSummaryTextView.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
        } else if (!state.mBtEnabled) {
            mInfoBox.setEmoji(":broken_heart:");
            mInfoBox.setHeading(R.string.ui_main_status_communicator_bt_disabled_heading);
            mInfoBox.setText(R.string.ui_main_status_communicator_bt_disabled_text);
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxWarning);
            showInfoBox = true;
        } else if (!state.mBleSupported) {
            mInfoBox.setEmoji(":dizzy_face:");
            mInfoBox.setHeading(mContext.getString(R.string.ui_main_status_communicator_ble_not_supported_heading));
            mInfoBox.setText(R.string.ui_main_status_communicator_ble_not_supported_text);
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxError);
            showInfoBox = true;
        } else if (!state.mShouldCommunicate) {
            showAuraOffInfoBox();
            showInfoBox = true;
        } else if (!state.mAdvertisingSupported) {
            mInfoBox.setEmoji(":broken_heart:");
            mInfoBox.setHeading(R.string.ui_main_status_communicator_advertising_not_supported_heading);
            mInfoBox.setText(R.string.ui_main_status_communicator_advertising_not_supported_text);
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxWarning);
            showInfoBox = true;
        } else if (!state.mAdvertising) {
            w(TAG, "Not advertising although it is possible.");
            mSummaryTextView.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_status_summary_communicator_on_not_active)));
            mSummaryTextView.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
        } else if (!state.mScanning) {
            w(TAG, "Not scanning although it is possible.");
            mSummaryTextView.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_status_summary_communicator_on_not_active)));
            mSummaryTextView.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
        } else {
            mSummaryTextView.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_status_summary_communicator_on)));
            mSummaryTextView.setBackgroundColor(mContext.getResources().getColor(R.color.green));
            mSummaryTextView.setTextColor(mContext.getResources().getColor(R.color.black));
        }
        if (showInfoBox) {
            mSummaryTextView.setVisibility(View.GONE);
            mInfoBox.setVisibility(View.VISIBLE);
        } else {
            mSummaryTextView.setVisibility(View.VISIBLE);
            mInfoBox.setVisibility(View.GONE);
        }
    }
}
