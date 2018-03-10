package io.auraapp.auraandroid.main.list.item;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.CuteHasher;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.main.InfoBox;
import io.auraapp.auraandroid.main.PeerSlogan;
import io.auraapp.auraandroid.main.list.RecycleAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.w;

public class StatusHolder extends ItemViewHolder {

    private final InfoBox mInfoBox;
    private final TextView mCommunicatorStateDumpView;

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
    private final Context mContext;
    private final boolean mExpanded;
    private final ListView mPeersListView;

    public StatusHolder(boolean expanded, Context context, View itemView, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);
        mExpanded = expanded;
        mContext = context;
        mSummaryTextView = itemView.findViewById(R.id.status_summary);
        mInfoBox = itemView.findViewById(R.id.info_box);
        mPeersListView = itemView.findViewById(R.id.peers_list);
        mCommunicatorStateDumpView = itemView.findViewById(R.id.communicator_state_dump);
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
        bindBasics(communicatorState);
        if (!mExpanded) {
            mPeersListView.setVisibility(View.GONE);
            mCommunicatorStateDumpView.setVisibility(View.GONE);
            return;
        }
        bindPeerList(peers);
        mPeersListView.setVisibility(View.VISIBLE);
        mCommunicatorStateDumpView.setVisibility(View.VISIBLE);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        mCommunicatorStateDumpView.setText(gson.toJson(castItem.mPeers).replaceAll("\"", ""));
    }

    private void bindBasics(CommunicatorState communicatorState) {

        String communicatorSummary = buildCommunicatorSummary(communicatorState);

        if (communicatorSummary == null) {
            mSummaryTextView.setVisibility(View.GONE);
            // buildCommunicatorSummary() mutates infobox exactly if returning null
            mInfoBox.setVisibility(View.VISIBLE);
            return;
        }

        mInfoBox.setVisibility(View.GONE);
        mSummaryTextView.setVisibility(View.VISIBLE);
        mSummaryTextView.setText(EmojiHelper.replaceShortCode(communicatorSummary));
    }

    @Nullable
    private String buildCommunicatorSummary(CommunicatorState state) {
        // The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
        if (state.mBluetoothRestartRequired) {
            mInfoBox.setEmoji(":warning:");
            mInfoBox.setHeading(R.string.ui_main_status_communicator_bt_restart_required_heading);
            mInfoBox.setText(R.string.ui_main_status_communicator_bt_restart_required_text);
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxError);
            return null;

        }
        if (state.mBtTurningOn) {
            return mContext.getString(R.string.ui_main_status_summary_communicator_bt_turning_on);
        }
        if (!state.mBtEnabled) {
            mInfoBox.setEmoji(":warning:");
            mInfoBox.setHeading(R.string.ui_main_status_communicator_bt_disabled_heading);
            mInfoBox.setText(R.string.ui_main_status_communicator_bt_disabled_text);
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxWarning);
            return null;
        }
        if (!state.mBleSupported) {
            mInfoBox.setEmoji(":dizzy_face:");
            mInfoBox.setHeading(mContext.getString(R.string.ui_main_status_communicator_ble_not_supported_heading));
            mInfoBox.setText(R.string.ui_main_status_communicator_ble_not_supported_text);
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxError);
            return null;
        }
        if (!state.mShouldCommunicate) {
            mInfoBox.setEmoji(":sleeping_sign:");
            mInfoBox.setHeading(R.string.ui_main_status_communicator_disabled_heading);
            mInfoBox.setText(R.string.ui_main_status_communicator_disabled_text);
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxWarning);
            return null;
        }
        if (!state.mAdvertisingSupported) {
            mInfoBox.setEmoji(":broken_heart:");
            mInfoBox.setHeading(R.string.ui_main_status_communicator_advertising_not_supported_heading);
            mInfoBox.setText(R.string.ui_main_status_communicator_advertising_not_supported_text);
            mInfoBox.hideButton();
            mInfoBox.setColor(R.color.infoBoxWarning);
            return null;
        }
        if (!state.mAdvertising) {
            w(TAG, "Not advertising although it is possible.");
            return mContext.getString(R.string.ui_main_status_summary_communicator_on_not_active);
        }
        if (!state.mScanning) {
            w(TAG, "Not scanning although it is possible.");
            return mContext.getString(R.string.ui_main_status_summary_communicator_on_not_active);
        }
        return mContext.getString(R.string.ui_main_status_summary_communicator_on);
    }

    private void bindPeerList(Set<Peer> peers) {

        // TODO don't recreate everything, cache? maybe not #onlyfordebugging
        mPeersListView.setAdapter(new PeerArrayAdapter(
                mContext,
                android.R.layout.simple_list_item_1,
                peers.toArray(new Peer[peers.size()])
        ));
    }
}
