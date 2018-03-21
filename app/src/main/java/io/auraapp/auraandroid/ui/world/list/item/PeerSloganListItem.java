package io.auraapp.auraandroid.ui.world.list.item;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

public class PeerSloganListItem extends ListItem {

    private final Slogan mSlogan;
    private final Set<Peer> mPeers;

    public PeerSloganListItem(@NonNull Slogan slogan, @NonNull Set<Peer> peers) {
        super("peer-slogan-" + slogan.getText());
        mSlogan = slogan;
        mPeers = peers;
    }

    @Override
    public void updateWith(@NonNull ListItem newItem) {
        if (!(newItem instanceof PeerSloganListItem)) {
            throw new RuntimeException("Cannot update " + PeerSloganListItem.class.getSimpleName() + " with " + newItem.getClass().getSimpleName());
        }
        mPeers.clear();
        mPeers.addAll(((PeerSloganListItem) newItem).getPeers());
    }

    @Nullable
    Set<Peer> getPeers() {
        return mPeers;
    }

    public Slogan getSlogan() {
        return mSlogan;
    }

    public long getLastSeen() {
        if (mPeers == null) {
            throw new RuntimeException("Cannot determine lastSeen for item that has no peers");
        }
        long lastSeen = 0;
        for (Peer peer : mPeers) {
            if (peer.mLastSeenTimestamp > lastSeen) {
                lastSeen = peer.mLastSeenTimestamp;
            }
        }
        return lastSeen;
    }
}
