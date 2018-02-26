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
}
