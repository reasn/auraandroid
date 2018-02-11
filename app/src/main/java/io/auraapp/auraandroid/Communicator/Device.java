package io.auraapp.auraandroid.Communicator;

import android.bluetooth.BluetoothDevice;

import java.util.Locale;

import static java.lang.String.format;

class Device {

    Long lastFullRetrievalTimestamp = null;
    Long lastSeenTimestamp = null;
    Long lastConnectAttempt = null;

    final PeerStatsSet stats = new PeerStatsSet();

    PeerBtServiceSet bt = new PeerBtServiceSet();

    boolean isDiscoveringServices = false;

    boolean connected = false;
    boolean shouldDisconnect = false;
    int connectionAttempts = 0;

    boolean isFetchingSlogan = false;

    String slogan1;
    String slogan2;
    String slogan3;

    boolean slogan1fresh = false;
    boolean slogan2fresh = false;
    boolean slogan3fresh = false;

    private Device() {
    }

    static Device create(BluetoothDevice device) {
        Device peer = new Device();
        peer.bt.device = device;
        return peer;
    }

    String toLogString() {
        return format(
                Locale.ENGLISH,
                "lastFullRetrievalTimestamp: %d"
                        + ", lastSeenTimestamp: %d"
                        + ", lastConnectAttempt: %d"
                        + ", isDiscoveringServices: %s"
                        + ", bt: (%s)"
                        + ", connected: %s"
                        + ", shouldDisconnect: %s"
                        + ", connectionAttempts: %d"
                        + ", stats: (%s)"
                        + ", isFetchingSlogan: %s"
                        + ", slogan1: %s"
                        + ", slogan2: %s"
                        + ", slogan3: %s",
                lastFullRetrievalTimestamp,
                lastSeenTimestamp,
                lastConnectAttempt,
                isDiscoveringServices ? "yes" : "no",
                bt.toLogString(),
                connected ? "yes" : "no",
                shouldDisconnect ? "yes" : "no",
                connectionAttempts,
                stats.toLogString(),
                isFetchingSlogan ? "yes" : "no",
                slogan1 == null ? null : "set",
                slogan2 == null ? null : "set",
                slogan3 == null ? null : "set"
        );
    }
}
