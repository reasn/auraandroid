package io.auraapp.auraandroid.main.list;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.Set;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;

import static io.auraapp.auraandroid.common.FormattedLog.e;

class PeerExpandedHolder extends ItemViewHolder {

    private static final String TAG = "aura/list/" + PeerExpandedHolder.class.getSimpleName();
    private final TextView mSloganTextView;
    private final TextView mLastSeenTextView;
    private final Context mContext;

    PeerExpandedHolder(View itemView, Context context, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);

        mContext = context;

        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        mLastSeenTextView = itemView.findViewById(R.id.lastSeen);

        itemView.setOnClickListener((v) -> collapseExpandHandler.flip(mItem));
    }

    @Override
    void bind(ListItem item) {

        long lastSeen = 0;
        if (item == null) {
            e(TAG, "Trying to bind %s to null ListItem", PeerExpandedHolder.class.getSimpleName());
            return;
        }
        Set<Peer> peers = item.getPeers();
        if (peers == null) {
            e(TAG, "Trying to bind %s with peers=null to ListItem", PeerExpandedHolder.class.getSimpleName());
            return;
        }
        for (Peer peer : peers) {
            if (peer.mLastSeenTimestamp > lastSeen) {
                lastSeen = peer.mLastSeenTimestamp;
            }
        }

        // TODO prettify
        mLastSeenTextView.setText(
                mContext.getString(R.string.ui_main_peer_slogan_last_seen)
                        .replace("##elapsed##", (System.currentTimeMillis() - lastSeen) + "s")
        );

        mSloganTextView.setText(item.getSlogan().getText());
    }
}