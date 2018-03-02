package io.auraapp.auraandroid.main.list.item;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.CuteHasher;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.main.PeerSlogan;

public class PeersStateHolder extends ItemViewHolder {

    private static final String TAG = "aura/main/list/item/" + PeersStateHolder.class.getSimpleName();
    private final TextView mExplanationTextView;
    private final Context mContext;

    public PeersStateHolder(Context context, View itemView) {
        super(itemView);
        mContext = context;
        mExplanationTextView = itemView.findViewById(R.id.communicator_state_explanation);
    }

    public void bind(ListItem item) {
        if (!(item instanceof PeersStateItem)) {
            throw new RuntimeException("Expecting " + PeersStateItem.class.getSimpleName());
        }

        TreeMap<String, PeerSlogan> map = ((PeersStateItem) item).mPeerSloganMap;
        Set<Peer> peers = ((PeersStateItem) item).mPeers;
        if (map == null || peers == null) {
            return;
        }

        String text;
        if (map.size() == 0) {
            text = mContext.getString(R.string.ui_main_explanation_on_no_slogans);
        } else {
            text = mContext.getString(R.string.ui_main_explanation_on_peers).replaceAll("##slogans##", Integer.toString(map.size()));
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

        text += "\n" + peerString;

        mExplanationTextView.setText(EmojiHelper.replaceShortCode(text));
    }
}
