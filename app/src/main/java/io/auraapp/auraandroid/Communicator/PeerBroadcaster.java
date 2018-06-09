package io.auraapp.auraandroid.Communicator;

import android.os.Handler;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.Timer;

import static io.auraapp.auraandroid.common.FormattedLog.iv;
import static io.auraapp.auraandroid.common.FormattedLog.v;

class PeerBroadcaster {

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
    private final Timer.Debouncer mListDebouncer = new Timer.Debouncer(mTimer, DEBOUNCE);

    PeerBroadcaster(PeerListChangedCallback peerListChangedCallback, PeerChangedCallback peerChangedCallback) {
        mPeersListCallback = peerListChangedCallback;
        mPeerChangedCallback = peerChangedCallback;
    }

    /**
     * contentChanged differentiates between "peer seen again" and "color/name/slogan/... changed"
     */
    void propagatePeer(Device device, boolean contentChanged, int sloganCount) {
        if (contentChanged) {
            v(TAG, "Not debouncing because content changed");
            // Not debouncing because otherwise the contentChange parameter could get lost.
            // Then no notification (e.g. vibration) is sent to the user
            device.clearDebouncer();
            mPeerChangedCallback.peerChanged(buildPeer(device), true, sloganCount);
        } else {
            iv(TAG, "Debouncing propagation of peer");

            device.getDebouncer(mTimer, DEBOUNCE).debounce(
                    () -> mPeerChangedCallback.peerChanged(buildPeer(device), false, sloganCount)
            );
//            mPeerDebouncer.debounce(() -> mPeerChangedCallback.peerChanged(buildPeer(device), false, sloganCount));
        }
    }

    void propagatePeerList(DeviceMap deviceMap) {
        iv(TAG, "Debouncing propagation of peer list");
        mListDebouncer.debounce(() -> mPeersListCallback.peerListChanged(buildPeers(deviceMap)));
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
        DevicePeerProfile profile = device.buildProfile();

        peer.mLastSeenTimestamp = device.lastSeenTimestamp;
        peer.mSynchronizing = device.mSynchronizing || profile == null;
        peer.mSuccessfulRetrievals = device.stats.mSuccessfulRetrievals;
        peer.mErrors = device.stats.mErrors;
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
