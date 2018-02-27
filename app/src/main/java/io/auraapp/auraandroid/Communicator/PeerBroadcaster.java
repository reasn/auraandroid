package io.auraapp.auraandroid.Communicator;

import android.os.Handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

import static io.auraapp.auraandroid.common.FormattedLog.v;

class PeerBroadcaster {

    @FunctionalInterface
    interface ProximityCallback {
        void proximityChanged(Set<Peer> peers);
    }

    private static final String TAG = "communicator/peerBroadcaster";
    private static final int DEBOUNCE = 1000;

    private final Handler mHandler = new Handler();
    private final HashMap<String, Device> mDevices;
    private final ProximityCallback mProximityCallback;
    private long mLastPropagationTimestamp;
    private Map<String, Peer> mPeerMap;

    PeerBroadcaster(HashMap<String, Device> devices, ProximityCallback proximityCallback) {
        this.mDevices = devices;
        mProximityCallback = proximityCallback;
    }

    void propagateChanges(boolean setChanged) {

        mHandler.removeCallbacks(this::doPropagate);
        rebuildPeers(setChanged);

        final long now = System.currentTimeMillis();
        if (now - mLastPropagationTimestamp < DEBOUNCE) {
            mHandler.postDelayed(this::doPropagate, mLastPropagationTimestamp + DEBOUNCE - now);
        } else {
            doPropagate();
        }
    }

    private void doPropagate() {
        v(TAG, "Propagating changed peers");
        mProximityCallback.proximityChanged(getPeers());
        mLastPropagationTimestamp = System.currentTimeMillis();
    }

    Set<Peer> getPeers() {
        if (mPeerMap == null) {
            rebuildPeers(true);
        }
        return new HashSet<>(mPeerMap.values());
    }

    private void rebuildPeers(boolean setChanged) {
        // TODO reenable
//        if (setChanged || mPeerMap == null) {
            mPeerMap = new HashMap<>();
            for (String address : mDevices.keySet()) {
                Peer peer = new Peer();
                peer.mAddress = address;
                mPeerMap.put(address, peer);
//            }
        }

        for (String address : mDevices.keySet()) {
            final Device device = mDevices.get(address);
            final Peer peer = mPeerMap.get(address);
            peer.mLastSeenTimestamp = device.lastSeenTimestamp;
            peer.mNextFetch = device.nextFetch;
            peer.mSuccessfulRetrievals = device.stats.mSuccessfulRetrievals;
            peer.mSlogans.clear();
            for (String sloganText : device.getSlogans()) {
                peer.mSlogans.add(Slogan.create(sloganText));
            }
        }
    }
}
