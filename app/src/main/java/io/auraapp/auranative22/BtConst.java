package io.auraapp.auranative22;

import static android.bluetooth.BluetoothGatt.GATT_CONNECTION_CONGESTED;
import static android.bluetooth.BluetoothGatt.GATT_FAILURE;
import static android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;
import static android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION;
import static android.bluetooth.BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
import static android.bluetooth.BluetoothGatt.GATT_INVALID_OFFSET;
import static android.bluetooth.BluetoothGatt.GATT_READ_NOT_PERMITTED;
import static android.bluetooth.BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGatt.GATT_WRITE_NOT_PERMITTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING;

class BtConst {
    static String nameStatus(int status) {
        switch (status) {

            case GATT_SUCCESS:
                return "GATT_SUCCESS";
            case GATT_READ_NOT_PERMITTED:
                return "GATT_READ_NOT_PERMITTED";
            case GATT_WRITE_NOT_PERMITTED:
                return "GATT_WRITE_NOT_PERMITTED";
            case GATT_INSUFFICIENT_AUTHENTICATION:
                return "GATT_INSUFFICIENT_AUTHENTICATION";
            case GATT_REQUEST_NOT_SUPPORTED:
                return "GATT_REQUEST_NOT_SUPPORTED";
            case GATT_INSUFFICIENT_ENCRYPTION:
                return "GATT_INSUFFICIENT_ENCRYPTION";
            case GATT_INVALID_OFFSET:
                return "GATT_INVALID_OFFSET";
            case GATT_INVALID_ATTRIBUTE_LENGTH:
                return "GATT_INVALID_ATTRIBUTE_LENGTH";
            case GATT_CONNECTION_CONGESTED:
                return "GATT_CONNECTION_CONGESTED";
            case GATT_FAILURE:
                return "GATT_FAILURE";
            default:
                return "unknown GATT status: " + status;
        }
    }

    static String nameConnectionState(int state) {

        switch (state) {
            case STATE_DISCONNECTED:
                return "STATE_DISCONNECTED";

            case STATE_CONNECTING:
                return "STATE_CONNECTING";

            case STATE_CONNECTED:
                return "STATE_CONNECTED";

            case STATE_DISCONNECTING:
                return "STATE_DISCONNECTING";
            default:
                return "unknown connection state: " + state;
        }
    }
}
