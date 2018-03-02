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
import io.auraapp.auraandroid.main.list.RecycleAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.w;

public class StatusHolder extends ItemViewHolder {

    private static final String TAG = "aura/main/list/item/" + StatusHolder.class.getSimpleName();
    private final TextView mExplanationTextView;
    private final Context mContext;
    private final boolean mExpanded;

    public StatusHolder(boolean expanded, Context context, View itemView, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);
        mExpanded = expanded;
        mContext = context;
        mExplanationTextView = itemView.findViewById(R.id.communicator_state_explanation);
        itemView.setOnClickListener($ -> collapseExpandHandler.flip(mItem));
    }

    public void bind(ListItem item) {
        if (!(item instanceof StatusItem)) {
            throw new RuntimeException("Expecting " + StatusItem.class.getSimpleName());
        }
        StatusItem castItem = (StatusItem) item;
        CommunicatorState communicatorState = castItem.mState;
        TreeMap<String, PeerSlogan> peerSloganMap = castItem.mPeerSloganMap;
        Set<Peer> peers = castItem.mPeers;
        if (peerSloganMap == null || peers == null || communicatorState == null) {
            return;
        }
        if (mExpanded) {
            bindExpanded(peerSloganMap, peers);
        } else {
            bindCollapsed(communicatorState, peerSloganMap, peers);
        }
    }

    private void bindCollapsed(CommunicatorState communicatorState, TreeMap<String, PeerSlogan> peerSloganMap, Set<Peer> peers) {
        mExplanationTextView.setText(EmojiHelper.replaceShortCode(
                buildCommunicatorState(communicatorState) + " " + buildPeerOverview(peerSloganMap, peers)
        ));
    }

    private String buildCommunicatorState(CommunicatorState state) {
        String text;
        // The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
        if (state.mBluetoothRestartRequired) {
            text = mContext.getString(R.string.ui_main_status_headline_communicator_bt_restart_required);

        } else if (state.mBtTurningOn) {
            text = mContext.getString(R.string.ui_main_status_headline_communicator_bt_turning_on);

        } else if (!state.mBtEnabled) {
            text = mContext.getString(R.string.ui_main_status_headline_communicator_bt_disabled);

        } else if (!state.mBleSupported) {
            text = mContext.getString(R.string.ui_main_status_headline_communicator_ble_not_supported);

        } else if (!state.mShouldCommunicate) {
            text = mContext.getString(R.string.ui_main_status_headline_communicator_disabled);

        } else {
            if (!state.mAdvertisingSupported) {
                text = mContext.getString(R.string.ui_main_status_headline_communicator_advertising_not_supported);
            } else if (!state.mAdvertising) {
                w(TAG, "Not advertising although it is possible.");
                text = mContext.getString(R.string.ui_main_status_headline_communicator_on_not_active);
            } else if (!state.mScanning) {
                w(TAG, "Not scanning although it is possible.");
                text = mContext.getString(R.string.ui_main_status_headline_communicator_on_not_active);
            } else {
                text = mContext.getString(R.string.ui_main_status_headline_communicator_on);
            }
        }
        return text;
    }

    private String buildPeerOverview(TreeMap<String, PeerSlogan> peerSloganMap, Set<Peer> peers) {

//        int nearbyPeers = 0;
//        long now = System.currentTimeMillis();
//        for (Peer peer : peers) {
//            if (now - peer.mLastSeenTimestamp < 30000) {
//                nearbyPeers++;
//            }
//        }
        return mContext.getString(R.string.ui_main_status_headline_communicator_peers)
                .replace("##slogans##", peerSloganMap.size() + "")
                .replace("##peers##", peers.size()+ "");
//        long timeToNextFetch = Integer.MAX_VALUE;
//        long now = System.currentTimeMillis();
//        for (Peer peer : peers) {
//            timeToNextFetch = Math.min(timeToNextFetch, Math.round((peer.mNextFetch - now) / 1000));
//        }
    }

    // TODO should fill list view
    private void bindExpanded(TreeMap<String, PeerSlogan> peerSloganMap, Set<Peer> peers) {

        String peersText = mContext.getString(R.string.ui_main_status_headline_communicator_on).replaceAll("##slogans##", Integer.toString(peerSloganMap.size()));

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

        mExplanationTextView.setText(EmojiHelper.replaceShortCode(peersText));
    }
}
