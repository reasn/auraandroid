package io.auraapp.auraandroid.main.list.item;

public class PeersHeadingItem extends ListItem {

    public int mPeerCount;
    public int mSloganCount;

    public PeersHeadingItem(int peerCount, int sloganCount) {
        super("peers-heading");
        mPeerCount = peerCount;
        mSloganCount = sloganCount;
    }

    @Override
    public void updateWith(ListItem newItem) {
    }
}
