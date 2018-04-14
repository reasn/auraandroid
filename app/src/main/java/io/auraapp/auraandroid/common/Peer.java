package io.auraapp.auraandroid.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Peer implements Serializable {

    public final String mId;
    public int mSuccessfulRetrievals = 0;
    public long mLastSeenTimestamp = 0;
    public int mErrors = 0;

    public String mColor;
    public String mText;
    public String mName;

    public final List<Slogan> mSlogans = new ArrayList<>();
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
                ", mColor='" + mColor + '\'' +
                ", mText='" + mText + '\'' +
                ", mName='" + mName + '\'' +
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
        if (!mId.equals(peer.mId)) return false;
        if (mColor != null ? !mColor.equals(peer.mColor) : peer.mColor != null) return false;
        if (mText != null ? !mText.equals(peer.mText) : peer.mText != null) return false;
        if (mName != null ? !mName.equals(peer.mName) : peer.mName != null) return false;
        return mSlogans.equals(peer.mSlogans);
    }

    @Override
    public int hashCode() {
        int result = mId.hashCode();
        result = 31 * result + mSuccessfulRetrievals;
        result = 31 * result + (int) (mLastSeenTimestamp ^ (mLastSeenTimestamp >>> 32));
        result = 31 * result + mErrors;
        result = 31 * result + (mColor != null ? mColor.hashCode() : 0);
        result = 31 * result + (mText != null ? mText.hashCode() : 0);
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + mSlogans.hashCode();
        result = 31 * result + (mSynchronizing ? 1 : 0);
        return result;
    }
}
