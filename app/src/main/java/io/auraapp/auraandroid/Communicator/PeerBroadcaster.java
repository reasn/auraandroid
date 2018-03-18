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
    interface PeerListChangedCallback {
        void peerListChanged(Set<Peer> peers);
    }

    @FunctionalInterface
    interface PeerChangedCallback {
        void peerChanged(Peer peer, boolean contentAdded, int sloganCount);
    }

    private static final int DEBOUNCE = 3000;

    private final PeerListChangedCallback mPeersListCallback;
    private final PeerChangedCallback mPeerChangedCallback;
    private final Timer mTimer = new Timer(new Handler());

    PeerBroadcaster(PeerListChangedCallback peerListChangedCallback, PeerChangedCallback peerChangedCallback) {
        mPeersListCallback = peerListChangedCallback;
        mPeerChangedCallback = peerChangedCallback;
    }

    void propagatePeer(Device device, boolean contentAdded, int sloganCount) {
        if (contentAdded) {
            // Not debouncing because otherwise contentAdded can get lost and no notification
            // (e.g. vibration) is sent to the user
            mTimer.clear(device.mId);
            mPeerChangedCallback.peerChanged(buildPeer(device), true, sloganCount);
        } else {
            mTimer.debounce(device.mId, () -> mPeerChangedCallback.peerChanged(buildPeer(device), false, sloganCount), DEBOUNCE);
        }
    }

    void propagatePeerList(DeviceMap deviceMap) {
        mTimer.debounce(DEBOUNCE_ID_ALL_PEERS, () -> mPeersListCallback.peerListChanged(buildPeers(deviceMap)), DEBOUNCE);
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
        peer.mSynchronizing = device.mSynchronizing;
        peer.mSuccessfulRetrievals = device.stats.mSuccessfulRetrievals;
        peer.mErrors = device.stats.mErrors;
        for (String sloganText : device.buildSlogans()) {
            peer.mSlogans.add(Slogan.create(sloganText));
        }
        return peer;
    }
}
