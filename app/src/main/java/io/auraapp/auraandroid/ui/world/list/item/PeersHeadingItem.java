package io.auraapp.auraandroid.ui.world.list.item;

import java.util.Set;

import io.auraapp.auraandroid.common.Peer;

public class PeersHeadingItem extends ListItem {

    public Set<Peer> mPeers;
    public int mSloganCount;
    public boolean mScanning;
    public long mScanStartTimestamp;

    public PeersHeadingItem(Set<Peer> peers, int sloganCount) {
        super("peers-heading");
        mPeers = peers;
        mSloganCount = sloganCount;
    }

    @Override
    public void updateWith(ListItem newItem) {
    }
}