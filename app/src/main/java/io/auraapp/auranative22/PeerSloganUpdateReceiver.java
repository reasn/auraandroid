package io.auraapp.auranative22;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auranative22.Communicator.Peer;
import io.auraapp.auranative22.Communicator.Slogan;

import static io.auraapp.auranative22.Communicator.Communicator.INTENT_PEERS_CHANGED_PEERS;
import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.v;
import static io.auraapp.auranative22.FormattedLog.w;

public class PeerSloganUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "@aura/peerSloganReceiver";
    private final TreeSet<Slogan> mPeerSlogans;
    private final SloganListAdapter mAdapter;

    public PeerSloganUpdateReceiver(TreeSet<Slogan> peerSlogans, SloganListAdapter Adapter) {
        mPeerSlogans = peerSlogans;
        mAdapter = Adapter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        v(TAG, "onReceive, intent: %s", intent);

        Bundle extras = intent.getExtras();
        if (extras == null) {
            v(TAG, "Intent has no extras, ignoring it, intent: %s", intent);
            return;
        }

        @SuppressWarnings("unchecked")
        Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEERS_CHANGED_PEERS);

        if (peers == null) {
            w(TAG, "Peers payload is null, ignoring it, intent: %s", intent);
            return;
        }


        final Set<Slogan> uniqueSlogans = new HashSet<>();
        for (Peer peer : peers) {
            uniqueSlogans.addAll(peer.mSlogans);
        }

        v(TAG, "Syncing %d previous slogans to %d slogans from %d peers", mPeerSlogans.size(), uniqueSlogans.size(), peers.size());

        if (mPeerSlogans.retainAll(uniqueSlogans) || mPeerSlogans.addAll(uniqueSlogans)) {
            mAdapter.notifyDataSetChanged();
        }
    }
}
