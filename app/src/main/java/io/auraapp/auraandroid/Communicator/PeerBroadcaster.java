package io.auraapp.auraandroid.Communicator;

import android.os.Handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.Timer;

class PeerBroadcaster {

    @FunctionalInterface
    interface PeersChangedCallback {
        void peersChanged(Set<Peer> peers);
    }

    @FunctionalInterface
    interface PeerSeenCallback {
        void peerSeen(String address, long timestamp);
    }

    private static final int DEBOUNCE = 1000;
    private final HashMap<String, Device> mDevices;
    private final PeersChangedCallback mPeersChangedCallback;
    private final PeerSeenCallback mPeerSeenCallback;
    private final Timer mTimer = new Timer(new Handler());

    private final Map<String, Timer.Timeout> mLastSeenTimeoutMap = new HashMap<>();
    private Timer.Timeout mAllPeersTimeout;

    private Map<String, Peer> mPeerMap;

    PeerBroadcaster(HashMap<String, Device> devices, PeersChangedCallback peersChangedCallback, PeerSeenCallback peerSeenCallback) {
        mDevices = devices;
        mPeersChangedCallback = peersChangedCallback;
        mPeerSeenCallback = peerSeenCallback;
    }

    void propagateLastSeen(String address, long timestamp) {
        mTimer.clear(mLastSeenTimeoutMap.get(address));
        mLastSeenTimeoutMap.put(address, mTimer.set(() -> {
            mLastSeenTimeoutMap.remove(address);
            mPeerSeenCallback.peerSeen(address, timestamp);
        }, DEBOUNCE));
    }

    void propagateAllPeers() {
        mTimer.clear(mAllPeersTimeout);
        rebuildPeers();
        mAllPeersTimeout = mTimer.set(() -> mPeersChangedCallback.peersChanged(getPeers()), DEBOUNCE);
    }

    Set<Peer> getPeers() {
        if (mPeerMap == null) {
            rebuildPeers();
        }
        return new HashSet<>(mPeerMap.values());
    }

    private void rebuildPeers() {
        mPeerMap = new HashMap<>();
        for (String address : mDevices.keySet()) {
            final Device device = mDevices.get(address);

            final Peer peer = new Peer();
            peer.mAddress = address;

            peer.mLastSeenTimestamp = device.lastSeenTimestamp;
            peer.mNextFetch = device.nextFetch;
            peer.mSuccessfulRetrievals = device.stats.mSuccessfulRetrievals;
            for (String sloganText : device.getSlogans()) {
                peer.mSlogans.add(Slogan.create(sloganText));
            }
            mPeerMap.put(address, peer);
        }
    }
}
