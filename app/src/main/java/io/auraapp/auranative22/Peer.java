package io.auraapp.auranative22;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;

import java.util.Locale;

import static java.lang.String.format;

class Peer {

    Long lastFullRetrievalTimestamp = null;
    Long lastSeenTimestamp = null;
    Long lastConnectAttempt = null;
    Long lastServiceDiscoveryAttempt = null;

    BluetoothDevice device = null;
    BluetoothGatt gatt = null;
    BluetoothGattService service = null;

    boolean connected = false;
    boolean shouldDisconnect = false;
    int connectionAttempts = 0;
    int errors = 0;

    String slogan1;
    String slogan2;
    String slogan3;

    private Peer() {
    }

    static Peer create(BluetoothDevice device) {
        Peer peer = new Peer();
        peer.device = device;
        return peer;
    }

    String toLogString() {
        return format(
                Locale.ENGLISH,
                "lastFullRetrievalTimestamp: %d"
                        + ", lastSeenTimestamp: %d"
                        + ", lastConnectAttempt: %d"
                        + ", lastServiceDiscoveryAttempt: %d"
                        + ", device: %s"
                        + ", gatt: %s"
                        + ", service: %s"
                        + ", connected: %s"
                        + ", shouldDisconnect: %s"
                        + ", connectionAttempts: %d"
                        + ", errors: %d"
                        + ", slogan1: %s"
                        + ", slogan2: %s"
                        + ", slogan3: %s",
                lastFullRetrievalTimestamp,
                lastSeenTimestamp,
                lastConnectAttempt,
                lastServiceDiscoveryAttempt,
                device == null ? null : device.getAddress(),
                gatt == null ? null : "set",
                service == null ? null : "set",
                connected ? "yes" : "no",
                shouldDisconnect ? "yes" : "no",
                connectionAttempts,
                errors,
                slogan1 == null ? null : "set",
                slogan2 == null ? null : "set",
                slogan3 == null ? null : "set"
        );
    }
}
