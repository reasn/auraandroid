package io.auraapp.auraandroid.main;

import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

public class PeerSlogan {
    public final Slogan mSlogan;
    public final Set<Peer> mPeers = new TreeSet<>();

    PeerSlogan(Slogan slogan) {
        this.mSlogan = slogan;
    }
}
