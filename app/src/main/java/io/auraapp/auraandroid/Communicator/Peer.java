package io.auraapp.auraandroid.Communicator;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Peer implements Serializable {
    public Long mLastSeenTimestamp = null;
    public final Set<Slogan> mSlogans = new HashSet<>();

    // TODO move to Slogan ?
    public int mSuccessfulRetrievals = 0;
}
