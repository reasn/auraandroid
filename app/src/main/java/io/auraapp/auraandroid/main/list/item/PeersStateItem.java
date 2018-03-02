package io.auraapp.auraandroid.main.list.item;

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.main.PeerSlogan;

public class PeersStateItem extends ListItem {

    public TreeMap<String, PeerSlogan> mPeerSloganMap;
    public Set<Peer> mPeers;

    public PeersStateItem(Set<Peer> peers, TreeMap<String, PeerSlogan> peerSloganMap) {
        super("peers-state");
        mPeers = peers;
        mPeerSloganMap = peerSloganMap;
    }

    @Override
    public void updateWith(ListItem newItem) {
        throw new RuntimeException("Must never be invoked");
    }
}
