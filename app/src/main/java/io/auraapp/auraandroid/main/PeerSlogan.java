package io.auraapp.auraandroid.main;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

public class PeerSlogan {
    public final Slogan mSlogan;
    public final Set<Peer> mPeers = new HashSet<>();

    PeerSlogan(Slogan slogan) {
        this.mSlogan = slogan;
    }
}
