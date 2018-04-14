package io.auraapp.auraandroid.Communicator;

import android.os.Handler;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.Timer;

import static io.auraapp.auraandroid.common.FormattedLog.v;

class PeerBroadcaster {

    private static final String DEBOUNCE_ID_ALL_PEERS = "all-peers";
    private static final String TAG = "@aura/communicator/" + PeerBroadcaster.class.getSimpleName();

    @FunctionalInterface
    interface PeerListChangedCallback {
        void peerListChanged(Set<Peer> peers);
    }

    @FunctionalInterface
    interface PeerChangedCallback {
        void peerChanged(Peer peer, boolean contentChanged, int sloganCount);
    }

    private static final int DEBOUNCE = 3000;

    private final PeerListChangedCallback mPeersListCallback;
    private final PeerChangedCallback mPeerChangedCallback;
    private final Timer mTimer = new Timer(new Handler());

    PeerBroadcaster(PeerListChangedCallback peerListChangedCallback, PeerChangedCallback peerChangedCallback) {
        mPeersListCallback = peerListChangedCallback;
        mPeerChangedCallback = peerChangedCallback;
    }

    /**
     * contentChanged differentiates between "peer seen again" and "color/name/slogan/... changed"
     */
    void propagatePeer(Device device, boolean contentChanged, int sloganCount) {
        if (contentChanged) {
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
        DevicePeerProfile profile = device.buildProfile();
        if (profile != null) {
            peer.mColor = profile.getColor();
            peer.mName = profile.getName();
            peer.mText = profile.getText();
        }

        for (String sloganText : device.buildSlogans()) {
            peer.mSlogans.add(Slogan.create(sloganText));
        }

        v(TAG, "Built peer %s", peer.toString());
        return peer;
    }
}
