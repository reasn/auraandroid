package io.auraapp.auraandroid.ui.world.list;

import android.support.annotation.NonNull;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.common.lists.ListItem;

public class PeerItem extends ListItem {

    private Peer mPeer;

    public PeerItem(@NonNull Peer peer) {
        super("peer-" + peer.mId + "-" + peer.mName);
        mPeer = peer;
    }

    @Override
    public void updateWith(@NonNull ListItem newItem) {
        if (!(newItem instanceof PeerItem)) {
            throw new RuntimeException("Cannot update " + PeerItem.class.getSimpleName() + " with " + newItem.getClass().getSimpleName());
        }
        mPeer = ((PeerItem) newItem).mPeer;
    }

    Peer getPeer() {
        return mPeer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PeerItem peerItem = (PeerItem) o;

        return mPeer != null ? mPeer.equals(peerItem.mPeer) : peerItem.mPeer == null;
    }

    @Override
    public int hashCode() {
        return mPeer != null ? mPeer.hashCode() : 0;
    }
}
