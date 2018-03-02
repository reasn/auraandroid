package io.auraapp.auraandroid.main.list.item;

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.main.PeerSlogan;

public class StatusItem extends ListItem {

    public CommunicatorState mState;
    public TreeMap<String, PeerSlogan> mPeerSloganMap;
    public Set<Peer> mPeers;

    public StatusItem(CommunicatorState state, Set<Peer> peers, TreeMap<String, PeerSlogan> peerSloganMap) {
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
