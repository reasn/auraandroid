package io.auraapp.auraandroid.main.list.item;

import android.support.annotation.Nullable;

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.main.PeerSlogan;

public class StatusItem extends ListItem {

    @Nullable
    public CommunicatorState mState;
    public TreeMap<String, PeerSlogan> mPeerSloganMap;
    public Set<Peer> mPeers;

    public StatusItem(@Nullable CommunicatorState state, Set<Peer> peers, TreeMap<String, PeerSlogan> peerSloganMap) {
        super("status");
        mState = state;
        mPeers = peers;
        mPeerSloganMap = peerSloganMap;
    }


    @Override
    public void updateWith(ListItem newItem) {
        throw new RuntimeException("Must never be invoked");
    }
}
