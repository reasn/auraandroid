package io.auraapp.auraandroid.ui.world;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

public class PeerSlogan {
    public final Slogan mSlogan;
    public final Set<Peer> mPeers = new HashSet<>();

    public PeerSlogan(Slogan slogan) {
        this.mSlogan = slogan;
    }

//    boolean hasPeerWithAddress(String address) {
//        for (Peer candidate : mPeers) {
//            if (candidate.mId.equals(mId)) {
//                return true;
//            }
//        }
//        return false;
//    }
}
