package io.auraapp.auraandroid.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Peer implements Serializable {

    public final String mId;
    public int mSuccessfulRetrievals = 0;
    public long mLastSeenTimestamp = 0;
    public int mErrors = 0;

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
                ", mLastSeenTimestamp=" + mLastSeenTimestamp +
                ", mErrors=" + mErrors +
                ", mSlogans=" + mSlogans +
                ", mSynchronizing=" + mSynchronizing +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peer peer = (Peer) o;

        if (mSuccessfulRetrievals != peer.mSuccessfulRetrievals) return false;
        if (mLastSeenTimestamp != peer.mLastSeenTimestamp) return false;
        if (mErrors != peer.mErrors) return false;
        if (mSynchronizing != peer.mSynchronizing) return false;
        if (mId != null ? !mId.equals(peer.mId) : peer.mId != null) return false;
        return mSlogans != null ? mSlogans.equals(peer.mSlogans) : peer.mSlogans == null;
    }

    @Override
    public int hashCode() {
        int result = mId != null ? mId.hashCode() : 0;
        result = 31 * result + mSuccessfulRetrievals;
        result = 31 * result + (int) (mLastSeenTimestamp ^ (mLastSeenTimestamp >>> 32));
        result = 31 * result + mErrors;
        result = 31 * result + (mSlogans != null ? mSlogans.hashCode() : 0);
        result = 31 * result + (mSynchronizing ? 1 : 0);
        return result;
    }
}
