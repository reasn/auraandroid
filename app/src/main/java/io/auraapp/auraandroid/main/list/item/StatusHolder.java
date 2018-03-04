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
    private final TextView mSummaryTextView;
    private final TextView mHeadingTextView;
    private final Context mContext;
    private final boolean mExpanded;
    private TextView mInfoTextView;

    public StatusHolder(boolean expanded, Context context, View itemView, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);
        mExpanded = expanded;
        mContext = context;
        mSummaryTextView = itemView.findViewById(R.id.status_summary);
        mHeadingTextView = itemView.findViewById(R.id.status_heading);
        mInfoTextView = itemView.findViewById(R.id.communicator_state_info);
        itemView.setOnClickListener($ -> collapseExpandHandler.flip(getLastBoundItem()));
    }

    public void bind(ListItem item, View itemView) {
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
            bindExpanded(communicatorState, peerSloganMap, peers);
        } else {
            bindCollapsed(communicatorState, peerSloganMap, peers);
        }
    }

    private void bindCollapsed(CommunicatorState communicatorState, TreeMap<String, PeerSlogan> peerSloganMap, Set<Peer> peers) {

        String[] communicatorResult = buildCommunicatorState(communicatorState);
        String communicatorSummary = communicatorResult[0];
        String communicatorInfo = communicatorResult[1];

        String[] peersResult = buildPeerOverview(peerSloganMap, peers);
        String peersSummary = peersResult[0];
        String peersInfo = peersResult[1];

        if (communicatorInfo == null && peersInfo == null) {
            mSummaryTextView.setText(EmojiHelper.replaceShortCode(communicatorSummary + " " + peersSummary));
            mSummaryTextView.setVisibility(View.VISIBLE);
            mInfoTextView.setVisibility(View.GONE);
            mHeadingTextView.setVisibility(View.GONE);
            return;
        }

        mHeadingTextView.setText(EmojiHelper.replaceShortCode(communicatorSummary));

        // communicatorState is more important than peersInfo because impacting proper operation of the app
        mInfoTextView.setText(EmojiHelper.replaceShortCode(
                communicatorInfo != null
                        ? communicatorInfo
                        : peersInfo
        ));

        mSummaryTextView.setVisibility(View.GONE);
        mHeadingTextView.setVisibility(View.VISIBLE);
        mInfoTextView.setVisibility(View.VISIBLE);
    }

    private String[] buildCommunicatorState(CommunicatorState state) {
        String summary;
        String info = null;
        // The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
        if (state.mBluetoothRestartRequired) {
            summary = mContext.getString(R.string.ui_main_status_summary_communicator_bt_restart_required);
            info = mContext.getString(R.string.ui_main_status_info_communicator_bt_restart_required);

        } else if (state.mBtTurningOn) {
            summary = mContext.getString(R.string.ui_main_status_summary_communicator_bt_turning_on);

        } else if (!state.mBtEnabled) {
            summary = mContext.getString(R.string.ui_main_status_summary_communicator_bt_disabled);
            info = mContext.getString(R.string.ui_main_status_info_communicator_bt_disabled);

        } else if (!state.mBleSupported) {
            summary = mContext.getString(R.string.ui_main_status_summary_communicator_ble_not_supported);
            info = mContext.getString(R.string.ui_main_status_info_communicator_ble_not_supported);

        } else if (!state.mShouldCommunicate) {
            summary = mContext.getString(R.string.ui_main_status_summary_communicator_disabled);
            info = mContext.getString(R.string.ui_main_status_info_communicator_disabled);

        } else {
            if (!state.mAdvertisingSupported) {
                summary = mContext.getString(R.string.ui_main_status_summary_communicator_advertising_not_supported);
                info = mContext.getString(R.string.ui_main_status_info_communicator_advertising_not_supported);
            } else if (!state.mAdvertising) {
                w(TAG, "Not advertising although it is possible.");
                summary = mContext.getString(R.string.ui_main_status_summary_communicator_on_not_active);
            } else if (!state.mScanning) {
                w(TAG, "Not scanning although it is possible.");
                summary = mContext.getString(R.string.ui_main_status_summary_communicator_on_not_active);
            } else {
                summary = mContext.getString(R.string.ui_main_status_summary_communicator_on);
            }
        }
        return new String[]{summary, info};
    }

    private String[] buildPeerOverview(TreeMap<String, PeerSlogan> peerSloganMap, Set<Peer> peers) {
        int nearbyPeers = 0;
        long now = System.currentTimeMillis();
        for (Peer peer : peers) {
            if (now - peer.mLastSeenTimestamp < 30000) {
                nearbyPeers++;
            }
        }
        boolean fetching = false;
        for (Peer peer : peers) {
            if (peer.mNextFetch <= now) {
                fetching = true;
            }
        }
        String summary = mContext.getResources().getQuantityString(
                fetching
                        ? R.plurals.ui_main_status_summary_peers_fetching
                        : R.plurals.ui_main_status_summary_peers,
                nearbyPeers,
                nearbyPeers);

        String info = peers.size() == 0
                ? mContext.getString(R.string.ui_main_status_summary_peers_no_peers_text)
                : null;

        return new String[]{summary, info};
    }

    // TODO should fill list view
    private void bindExpanded(CommunicatorState state, TreeMap<String, PeerSlogan> peerSloganMap, Set<Peer> peers) {

        String peersText = mContext.getString(R.string.ui_main_status_summary_communicator_on).replaceAll("##slogans##", Integer.toString(peerSloganMap.size()));

        StringBuilder peerString = new StringBuilder();
        long now = System.currentTimeMillis();
        peerString.append("\nMe: ")
                .append(CuteHasher.hash(state.mId + ""))
                .append(" (")
                .append(((int) state.mVersion))
                .append(")\n");
        for (Peer peer : peers) {
            if (peerString.length() > 0) {
                peerString.append(", ");
            }
            peerString.append(CuteHasher.hash(peer.mId))
                    .append(": ");

            int timeToNextFetch = Math.round((peer.mNextFetch - now) / 1000);
            if (timeToNextFetch < 1) {
                peerString.append("fetching");
            } else {
                peerString.append(timeToNextFetch)
                        .append("s");

            }
        }

        peersText += "\n" + peerString;

        mSummaryTextView.setText(EmojiHelper.replaceShortCode(peersText));
    }
}
