package io.auraapp.auraandroid.main.list.item;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
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

    class PeerArrayAdapter extends ArrayAdapter<Peer> {
        public PeerArrayAdapter(@NonNull Context context, int resource, @NonNull Peer[] peers) {
            super(context, resource, peers);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Peer peer = getItem(position);
            if (peer == null) {
                throw new RuntimeException("Peer must not be null");
            }
            long now = System.currentTimeMillis();
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.status_list_item, parent, false);
            }
            TextView emojiTextView = convertView.findViewById(R.id.emoji);
            TextView detailsTextView = convertView.findViewById(R.id.details);

            emojiTextView.setText(
                    CuteHasher.hash(peer.mId));

            String text = "";

            int timeToNextFetch = Math.round((peer.mNextFetch - now) / 1000);
            if (timeToNextFetch < 1) {
                text += "fetching";
            } else {
                text += "next sync: " + timeToNextFetch + "s";
            }
            text += ", retrievals: " + peer.mSuccessfulRetrievals;
            text += ", last seen: " + Math.round((now - peer.mLastSeenTimestamp) / 1000) + "s ago";
// TODO implemente
            //            text += ", errors: " + peer.mErrors;

            detailsTextView.setText(text);

            return convertView;
        }
    }

    private static final String TAG = "aura/main/list/item/" + StatusHolder.class.getSimpleName();
    private final TextView mSummaryTextView;
    private final TextView mHeadingTextView;
    private final Context mContext;
    private final boolean mExpanded;
    private final LinearLayout mInfoWrapper;
    private final TextView mInfoTextView;
    private final ListView mPeersListView;

    public StatusHolder(boolean expanded, Context context, View itemView, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);
        mExpanded = expanded;
        mContext = context;
        mSummaryTextView = itemView.findViewById(R.id.status_summary);
        mInfoWrapper = itemView.findViewById(R.id.status_info_wrapper);
        mHeadingTextView = itemView.findViewById(R.id.status_heading);
        mInfoTextView = itemView.findViewById(R.id.status_info);
        mPeersListView = itemView.findViewById(R.id.peers_list);
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
        bindBasics(communicatorState, peerSloganMap, peers);
        if (mExpanded) {
            bindPeerList(communicatorState, peerSloganMap, peers);
            mPeersListView.setVisibility(View.VISIBLE);
        } else {
            mPeersListView.setVisibility(View.GONE);
        }
    }

    private void bindBasics(CommunicatorState communicatorState, TreeMap<String, PeerSlogan> peerSloganMap, Set<Peer> peers) {

        String[] communicatorResult = buildCommunicatorState(communicatorState);
        String communicatorSummary = communicatorResult[0];
        String communicatorInfo = communicatorResult[1];

        String[] peersResult = buildPeerOverview(peerSloganMap, peers);
        String peersSummary = peersResult[0];
        String peersInfo = peersResult[1];

        if (communicatorInfo == null && peersInfo == null) {
            mSummaryTextView.setText(EmojiHelper.replaceShortCode(communicatorSummary + " " + peersSummary));
            mSummaryTextView.setVisibility(View.VISIBLE);
            mInfoWrapper.setVisibility(View.GONE);
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
        mInfoWrapper.setVisibility(View.VISIBLE);
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
        String summary = mContext.getResources().getQuantityString(R.plurals.ui_main_status_summary_peers, nearbyPeers, nearbyPeers);

        String info = peers.size() == 0
                ? mContext.getString(R.string.ui_main_status_summary_peers_no_peers_text)
                : null;

        return new String[]{summary, info};
    }

    private void bindPeerList(CommunicatorState state, TreeMap<String, PeerSlogan> peerSloganMap, Set<Peer> peers) {

        // TODO don't recreate everything, cache? maybe not #onlyfordebugging
        mPeersListView.setAdapter(new PeerArrayAdapter(
                mContext,
                android.R.layout.simple_list_item_1,
                peers.toArray(new Peer[peers.size()])
        ));
    }

}
