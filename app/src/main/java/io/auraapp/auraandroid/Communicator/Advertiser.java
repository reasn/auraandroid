package io.auraapp.auraandroid.Communicator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;

class Advertiser {

    private final static String TAG = "@aura/ble/advertiser";
    private final UUID mSlogan3Uuid;
    private final UUID mSlogan1Uuid;
    private final UUID mSlogan2Uuid;
    private final UUID mServiceUuid;
    private final Communicator.OnBleSupportChangedCallback mOnBleSupportChangedCallback;
    private byte[] mSlogan1 = new byte[0];
    private byte[] mSlogan2 = new byte[0];
    private byte[] mSlogan3 = new byte[0];

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private Context mContext;
    private BluetoothLeAdvertiser mBluetoothAdvertiser;
    /**
     * Unrecoverable errors
     */
    boolean mUnrecoverableAdvertisingError = false;

    private AdvertiseCallback mAdvertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            d(TAG, "onStartSuccess, settings: %s", settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            e(TAG, "onStartFailure, errorCode: %s", BtConst.nameAdvertiseError(errorCode));
            advertisingUnsupported();

            mUnrecoverableAdvertisingError = true;
            mOnBleSupportChangedCallback.onBleSupportChanged();
        }
    };


    Advertiser(BluetoothManager bluetoothManager,
               UUID serviceUuid,
               UUID slogan1Uuid,
               UUID slogan2Uuid,
               UUID slogan3Uuid,
               Context context,
               Communicator.OnBleSupportChangedCallback onBleSupportChangedCallback) {
        mBluetoothManager = bluetoothManager;
        mServiceUuid = serviceUuid;
        mSlogan1Uuid = slogan1Uuid;
        mSlogan2Uuid = slogan2Uuid;
        mSlogan3Uuid = slogan3Uuid;
        mContext = context;
        mOnBleSupportChangedCallback = onBleSupportChangedCallback;
    }

    void setSlogan1(String slogan) {
        i(TAG, "Setting slogan 1 to %s", slogan);
        mSlogan1 = slogan == null
                ? new byte[0]
                : slogan.getBytes(Charset.forName("UTF-8"));
    }

    void setSlogan2(String slogan) {
        i(TAG, "Setting slogan 2 to %s", slogan);
        mSlogan2 = slogan == null
                ? new byte[0]
                : slogan.getBytes(Charset.forName("UTF-8"));
    }

    void setSlogan3(String slogan) {
        i(TAG, "Setting slogan 3 to %s", slogan);
        mSlogan3 = slogan == null
                ? new byte[0]
                : slogan.getBytes(Charset.forName("UTF-8"));
    }

    void start() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            advertisingUnsupported();
            return;
        }
        mBluetoothAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        if (mBluetoothAdvertiser == null) {
            advertisingUnsupported();
            return;
        }

        advertise();
        startServer();
    }

    void stop() {
        d(TAG, "Making sure advertising is stopped");
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
        }
        if (mBluetoothAdvertiser != null) {
            mBluetoothAdvertiser.stopAdvertising(mAdvertisingCallback);
        }
    }

    private void advertisingUnsupported() {
        d(TAG, "Advertising seems to be unsupported on this device");
        stop();
        // TODO implement
    }

    private void startServer() {

        BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                v(TAG, "onConnectionStateChange device: %s, status: %s, newState: %s", device.getAddress(), BtConst.nameStatus(status), BtConst.nameConnectionState(newState));
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

                v(TAG, "onCharacteristicReadRequest device: %s, requestId: %d, offset: %d, characteristic: %s", device.getAddress(), requestId, offset, characteristic.getUuid());

                if (mSlogan1Uuid.equals(characteristic.getUuid())) {
                    d(TAG, "sending slogan 1, bytes: %d", mSlogan1.length);
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk(mSlogan1, offset));

                } else if (mSlogan2Uuid.equals(characteristic.getUuid())) {
                    d(TAG, "sending slogan 2, bytes: %d", mSlogan2.length);
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk(mSlogan2, offset));

                } else if (mSlogan3Uuid.equals(characteristic.getUuid())) {
                    d(TAG, "sending slogan 3, bytes: %d", mSlogan3.length);
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk(mSlogan3, offset));

                } else {
                    // Invalid characteristic
                    w(TAG, "Invalid characteristic requested, device: %s, characteristic: %s", device.getAddress(), characteristic.getUuid());
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            }
        };

        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);

        assert mBluetoothGattServer != null;

        mBluetoothGattServer.addService(createSloganService());
    }

    /**
     * onCharacteristicReadRequest is called until no more response is transmitted.
     * For that to be achieved sendResponse() has to be eventually invoked with an empty byte[].
     * Otherwise the payload is transmitted multiple times, significantly reducing throughput.
     */
    private byte[] chunk(byte[] slogan, int offset) {
        if (offset > slogan.length) {
            return new byte[0];
        }
        return Arrays.copyOfRange(slogan, offset, slogan.length);
    }

    private BluetoothGattService createSloganService() {

        BluetoothGattService service = new BluetoothGattService(mServiceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        for (UUID uuid : new UUID[]{
                mSlogan1Uuid,
                mSlogan2Uuid,
                mSlogan3Uuid,
        }) {
            BluetoothGattCharacteristic chara = new BluetoothGattCharacteristic(uuid, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
            if (!service.addCharacteristic(chara)) {
                e(TAG, "Could not add characteristic");
                mUnrecoverableAdvertisingError = true;
                mOnBleSupportChangedCallback.onBleSupportChanged();
            }
        }
        return service;
    }


    /**
     * Inspiration
     * - https://code.tutsplus.com/tutorials/how-to-advertise-android-as-a-bluetooth-le-peripheral--cms-25426
     */
    private void advertise() {

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                // Disable timeout for advertising
                .setTimeout(0)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(mServiceUuid))
                .build();

        mBluetoothAdvertiser.startAdvertising(settings, data, mAdvertisingCallback);
        i(TAG, "started advertising");
    }
}
