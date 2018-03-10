package io.auraapp.auraandroid.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Peer implements Serializable {

    public final String mId;
    public int mSuccessfulRetrievals = 0;
    public long mNextFetch = 0;
    public long mLastSeenTimestamp = 0;

    public final Set<Slogan> mSlogans = new HashSet<>();
    public boolean mSynchronizing = false;

    public Peer(String id) {
        mId = id;
    }
//
//    public void updateWith(Peer peer) {
//        mLastSeenTimestamp = peer.mLastSeenTimestamp;
//        mSlogans.clear();
//        mSlogans.addAll(peer.mSlogans);
//        mSuccessfulRetrievals = peer.mSuccessfulRetrievals;
//        mNextFetch = peer.mNextFetch;
//    }

    @Override
    public String toString() {
        return "Peer{" +
                "mId='" + mId + '\'' +
                ", mSuccessfulRetrievals=" + mSuccessfulRetrievals +
                ", mNextFetch=" + mNextFetch +
                ", mLastSeenTimestamp=" + mLastSeenTimestamp +
                ", mSlogans=" + mSlogans +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peer peer = (Peer) o;

        if (mSuccessfulRetrievals != peer.mSuccessfulRetrievals) return false;
        if (mNextFetch != peer.mNextFetch) return false;
        if (mLastSeenTimestamp != peer.mLastSeenTimestamp) return false;
        if (!mId.equals(peer.mId)) return false;
        return mSlogans.equals(peer.mSlogans);
    }

    @Override
    public int hashCode() {
        int result = mId.hashCode();
        result = 31 * result + mSuccessfulRetrievals;
        result = 31 * result + (int) (mNextFetch ^ (mNextFetch >>> 32));
        result = 31 * result + (int) (mLastSeenTimestamp ^ (mLastSeenTimestamp >>> 32));
        result = 31 * result + mSlogans.hashCode();
        return result;
    }
}
