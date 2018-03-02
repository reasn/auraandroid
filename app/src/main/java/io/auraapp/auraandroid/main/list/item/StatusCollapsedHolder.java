package io.auraapp.auraandroid.main.list.item;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.CuteHasher;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.main.PeerSlogan;

import static io.auraapp.auraandroid.common.FormattedLog.w;

public class StatusCollapsedHolder extends ItemViewHolder {

    private static final String TAG = "aura/main/list/item/" + StatusCollapsedHolder.class.getSimpleName();
    private final TextView mExplanationTextView;
    private final Context mContext;

    public StatusCollapsedHolder(Context context, View itemView) {
        super(itemView);
        mContext = context;
        mExplanationTextView = itemView.findViewById(R.id.communicator_state_explanation);
    }

    public void bind(ListItem item) {
        if (!(item instanceof StatusItem)) {
            throw new RuntimeException("Expecting " + StatusItem.class.getSimpleName());
        }

        CommunicatorState state = ((StatusItem) item).mState;
        TreeMap<String, PeerSlogan> map = ((StatusItem) item).mPeerSloganMap;
        Set<Peer> peers = ((StatusItem) item).mPeers;
        if (map == null || peers == null || state == null) {
            return;
        }
        String text;
        // The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
        if (state.mBluetoothRestartRequired) {
            text = mContext.getString(R.string.ui_main_explanation_bt_restart_required);

        } else if (state.mBtTurningOn) {
            text = mContext.getString(R.string.ui_main_explanation_bt_turning_on);

        } else if (!state.mBtEnabled) {
            text = mContext.getString(R.string.ui_main_explanation_bt_disabled);

        } else if (!state.mBleSupported) {
            text = mContext.getString(R.string.ui_main_explanation_ble_not_supported);

        } else if (!state.mShouldCommunicate) {
            text = mContext.getString(R.string.ui_main_explanation_disabled);

        } else {
            if (!state.mAdvertisingSupported) {
                text = mContext.getString(R.string.ui_main_explanation_advertising_not_supported);
            } else if (!state.mAdvertising) {
                w(TAG, "Not advertising although it is possible.");
                text = mContext.getString(R.string.ui_main_explanation_on_not_active);
            } else if (!state.mScanning) {
                w(TAG, "Not scanning although it is possible.");
                text = mContext.getString(R.string.ui_main_explanation_on_not_active);
            } else {
                text = mContext.getString(R.string.ui_notification_on_title);
            }
        }

        String peersText;
        if (map.size() == 0) {
            peersText = mContext.getString(R.string.ui_main_explanation_on_no_slogans);
        } else {
            peersText = mContext.getString(R.string.ui_main_explanation_on_peers).replaceAll("##slogans##", Integer.toString(map.size()));
        }

        StringBuilder peerString = new StringBuilder();
        long now = System.currentTimeMillis();
        for (Peer peer : peers) {
            if (peerString.length() > 0) {
                peerString.append(", ");
            }
            peerString.append(CuteHasher.hash(peer.mAddress))
                    .append(": ");

            int timeToNextFetch = Math.round((peer.mNextFetch - now) / 1000);
            if (timeToNextFetch < 0) {
                peerString.append("fetching");
            } else {
                peerString.append(timeToNextFetch)
                        .append("s");

            }
        }

        peersText += "\n" + peerString;

        mExplanationTextView.setText(EmojiHelper.replaceShortCode(text + peersText));
    }
}
