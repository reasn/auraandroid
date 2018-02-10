package io.auraapp.auranative22;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.auraapp.auranative22.Communicator.Peer;
import io.auraapp.auranative22.Communicator.Slogan;

import static io.auraapp.auranative22.Communicator.Communicator.INTENT_PEERS_CHANGED_PEERS;
import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.v;

public class PeerSloganUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "@aura/peerSloganReceiver";
    private final List<ListItem> mList;
    private final ArrayAdapter<ListItem> mAdapter;

    public PeerSloganUpdateReceiver(List<ListItem> List, ArrayAdapter<ListItem> Adapter) {
        mList = List;
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
        Set<Peer> candidate = (Set<Peer>) extras.getSerializable(INTENT_PEERS_CHANGED_PEERS);

        final Set<Peer> peers = candidate != null
                ? candidate
                : new HashSet<>();

        final Set<Slogan> uniqueSlogans = new HashSet<>();
        for (Peer peer : peers) {
            uniqueSlogans.addAll(peer.mSlogans);
        }

        int found = 0;
        int gone = 0;

        // Remove slogans that are gone
        for (ListItem item : mList.subList(0, mList.size())) {
            if (!uniqueSlogans.contains(item.getSlogan())) {
                mList.remove(item);
                gone++;
            }
        }

        for (Slogan slogan : uniqueSlogans) {
            ListItem foundSlogan = new ListItem(slogan, false);
            if (!mList.contains(foundSlogan)) {
                mList.add(foundSlogan);
                found++;
            }
        }

        if (found == 0 && gone == 0) {
            v(TAG, "Received updated peers but nothing changed, peers: %d, unique logans: %d", peers.size(), uniqueSlogans.size());
        } else {
            d(TAG, "Received updated peers, %d slogans found, %d slogans gone, peers: %d, unique logans: %d", found, gone, peers.size(), uniqueSlogans.size());
            mAdapter.notifyDataSetChanged();
        }
    }
}
