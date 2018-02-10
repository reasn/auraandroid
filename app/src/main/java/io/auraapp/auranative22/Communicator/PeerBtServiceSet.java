package io.auraapp.auranative22.Communicator;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;

import java.util.Locale;

import static java.lang.String.format;

class PeerBtServiceSet {
    BluetoothDevice device = null;
    BluetoothGatt gatt = null;
    BluetoothGattService service = null;

    String toLogString() {
        return format(
                Locale.ENGLISH,
                "device: %s"
                        + ", gatt: %s"
                        + ", service: %s",
                device == null ? null : device.getAddress(),
                gatt == null ? null : "set",
                service == null ? null : "set"
        );
    }
}
