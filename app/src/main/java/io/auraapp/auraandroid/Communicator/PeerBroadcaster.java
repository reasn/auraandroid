package io.auraapp.auraandroid.Communicator;

import android.os.Handler;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.Timer;

class PeerBroadcaster {

    private static final String DEBOUNCE_ID_ALL_PEERS = "all-peers";

    @FunctionalInterface
    interface PeersChangedCallback {
        void peersChanged(Set<Peer> peers);
    }

    @FunctionalInterface
    interface PeerChangedCallback {
        void peerChanged(Peer peer);
    }

    private static final int DEBOUNCE = 3000;

    private final PeersChangedCallback mPeersListCallback;
    private final PeerChangedCallback mPeerChangedCallback;
    private final Timer mTimer = new Timer(new Handler());

    PeerBroadcaster(PeersChangedCallback peersChangedCallback, PeerChangedCallback peerChangedCallback) {
        mPeersListCallback = peersChangedCallback;
        mPeerChangedCallback = peerChangedCallback;
    }

    void propagatePeer(Device device) {
        mTimer.debounce(device.mId, () -> mPeerChangedCallback.peerChanged(buildPeer(device)), DEBOUNCE);
    }

    void propagatePeerList(DeviceMap deviceMap) {
        mTimer.debounce(DEBOUNCE_ID_ALL_PEERS, () -> mPeersListCallback.peersChanged(buildPeers(deviceMap)), DEBOUNCE);
    }

    Set<Peer> buildPeers(DeviceMap deviceMap) {
        Set<Peer> peers = new HashSet<>();
        for (Device device : deviceMap.values()) {
            peers.add(buildPeer(device));
        }
        return peers;
    }

    private Peer buildPeer(Device device) {
        final Peer peer = new Peer(device.mId);

        peer.mLastSeenTimestamp = device.lastSeenTimestamp;
        peer.mNextFetch = device.mNextFetch;
        peer.mSuccessfulRetrievals = device.stats.mSuccessfulRetrievals;
        for (String sloganText : device.getSlogans()) {
            peer.mSlogans.add(Slogan.create(sloganText));
        }
        return peer;
    }
}
