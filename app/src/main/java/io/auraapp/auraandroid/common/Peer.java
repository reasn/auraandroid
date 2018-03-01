package io.auraapp.auraandroid.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Peer implements Serializable {
    public long mLastSeenTimestamp = 0;
    public final Set<Slogan> mSlogans = new HashSet<>();

    public int mSuccessfulRetrievals = 0;
    public long mNextFetch = 0;
    public String mAddress;

    public void updateWith(Peer peer) {
        mLastSeenTimestamp = peer.mLastSeenTimestamp;
        mSlogans.clear();
        mSlogans.addAll(peer.mSlogans);
        mSuccessfulRetrievals = peer.mSuccessfulRetrievals;
        mNextFetch = peer.mNextFetch;
        mAddress = peer.mAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peer peer = (Peer) o;

        if (mLastSeenTimestamp != peer.mLastSeenTimestamp) return false;
        if (mSuccessfulRetrievals != peer.mSuccessfulRetrievals) return false;
        if (mNextFetch != peer.mNextFetch) return false;
        if (!mSlogans.equals(peer.mSlogans)) return false;
        return mAddress != null ? mAddress.equals(peer.mAddress) : peer.mAddress == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (mLastSeenTimestamp ^ (mLastSeenTimestamp >>> 32));
        result = 31 * result + mSlogans.hashCode();
        result = 31 * result + mSuccessfulRetrievals;
        result = 31 * result + (int) (mNextFetch ^ (mNextFetch >>> 32));
        result = 31 * result + (mAddress != null ? mAddress.hashCode() : 0);
        return result;
    }

}
