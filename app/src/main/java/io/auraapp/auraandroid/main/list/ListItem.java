package io.auraapp.auraandroid.main.list;

import android.support.annotation.Nullable;

import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

public class ListItem {
    private final Slogan mSlogan;
    @Nullable
    private final Set<Peer> mPeers;
    boolean mExpanded = false;

    ListItem(Slogan slogan, @Nullable Set<Peer> peers) {
        mSlogan = slogan;
        mPeers = peers;
    }

    Slogan getSlogan() {
        return mSlogan;
    }

    boolean isMine() {
        return mPeers == null;
    }

    @Nullable
    public Set<Peer> getPeers() {
        return mPeers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListItem listItem = (ListItem) o;

        if (mExpanded != listItem.mExpanded) return false;
        if (mSlogan != null ? !mSlogan.equals(listItem.mSlogan) : listItem.mSlogan != null)
            return false;
        return mPeers != null ? mPeers.equals(listItem.mPeers) : listItem.mPeers == null;
    }

    @Override
    public int hashCode() {
        int result = mSlogan != null ? mSlogan.hashCode() : 0;
        result = 31 * result + (mPeers != null ? mPeers.hashCode() : 0);
        result = 31 * result + (mExpanded ? 1 : 0);
        return result;
    }
}
