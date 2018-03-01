package io.auraapp.auraandroid.common;

public class PeerStatsSet {

    public int mSuccessfulRetrievals = 0;
    public int mErrors = 0;

    @Override
    public String toString() {
        return "PeerStatsSet{" +
                "mSuccessfulRetrievals=" + mSuccessfulRetrievals +
                ", mErrors=" + mErrors +
                '}';
    }
}
