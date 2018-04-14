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
import android.os.Handler;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.Timer;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static io.auraapp.auraandroid.Communicator.MetaDataUnpacker.byteArrayToString;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;

/**
 * All methods accessible from the outside post a callback to mHandler to avoid
 * concurrent modification of any class properties.
 * The same holds for all callbacks registered externally (in this case typically the BT stack).
 */
class Advertiser {

    @FunctionalInterface
    interface StateChangeCallback {
        void onStateChange(byte version, int id);
    }

    private final static String TAG = "@aura/ble/advertiser";

    private final BluetoothManager mBluetoothManager;
    private final AdvertisementSet mAdvertisementSet;
    private final Context mContext;
    private final StateChangeCallback mStateChangeCallback;
    private final Communicator.OnErrorCallback mOnErrorCallback;
    private final Handler mHandler = new Handler();
    private final Timer mTimer = new Timer(mHandler);

    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothAdvertiser;

    private final AdvertiseCallback mAdvertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            d(TAG, "onStartSuccess, settings: %s", settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            e(TAG, "onStartFailure, errorCode: %s", BtConst.nameAdvertiseError(errorCode));
            mHandler.post(() -> advertisingUnsupported(false, BtConst.nameAdvertiseError(errorCode)));
        }
    };

    Advertiser(BluetoothManager bluetoothManager,
               AdvertisementSet advertisementSet,
               Context context,
               StateChangeCallback stateChangeCallback,
               Communicator.OnErrorCallback onErrorCallback) {
        mBluetoothManager = bluetoothManager;
        mAdvertisementSet = advertisementSet;
        mContext = context;
        mStateChangeCallback = stateChangeCallback;
        mOnErrorCallback = onErrorCallback;
    }

    @ExternalInvocation
    void start() {
        mHandler.post(() -> {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                advertisingUnsupported(true, "no adapter");
                return;
            }
            mBluetoothAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

            if (mBluetoothAdvertiser == null) {
                advertisingUnsupported(true, "no adapter");
                return;
            }

            shuffleId();
            advertise();
            startServer();
            mTimer.setSerializedInterval("shuffle-id", () -> {
                shuffleId();
                mBluetoothAdvertiser.stopAdvertising(mAdvertisingCallback);
                advertise();
            }, Config.COMMUNICATOR_MY_ID_SHUFFLE_INTERVAL);
        });
    }

    void updateAdvertisement() {
        i(TAG, "Updating advertisement (stopping and starting)");
        mBluetoothAdvertiser.stopAdvertising(mAdvertisingCallback);
        advertise();
        mStateChangeCallback.onStateChange(mAdvertisementSet.mVersion, mAdvertisementSet.mId);
    }


    private void shuffleId() {
        mAdvertisementSet.shuffleId();
        mStateChangeCallback.onStateChange(mAdvertisementSet.mVersion, mAdvertisementSet.mId);
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

        byte[] metaData = mAdvertisementSet.getMetaData();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addServiceUuid(UuidSet.SERVICE_PARCEL)
                .addServiceData(UuidSet.SERVICE_DATA_PARCEL, metaData)
                .build();

        mBluetoothAdvertiser.startAdvertising(settings, data, mAdvertisingCallback);
        i(TAG, "started advertising, id: %s, version: %s, meta: %s, slogans: %d, service: %s",
                Integer.toHexString(mAdvertisementSet.mId),
                byteArrayToString(metaData),
                mAdvertisementSet.mSlogans.length,
                mAdvertisementSet.mVersion,
                UuidSet.SERVICE);
    }

    @ExternalInvocation
    void stop() {
        mHandler.post(() -> {
            d(TAG, "Making sure advertising is stopped");
            mTimer.clear("shuffle-id");
            if (mBluetoothGattServer != null) {
                mBluetoothGattServer.clearServices();
                try {
                    mBluetoothGattServer.close();
                } finally {
                    mBluetoothGattServer = null;

                    if (mBluetoothAdvertiser != null) {
                        try {
                            mBluetoothAdvertiser.stopAdvertising(mAdvertisingCallback);
                        } finally {
                            mBluetoothAdvertiser = null;
                        }
                    }
                }
            }
        });
    }

    private void advertisingUnsupported(boolean recoverable, String errorName) {
        d(TAG, "Advertising seems to be unsupported on this device");
        stop();
        if (!recoverable) {
            mOnErrorCallback.onUnrecoverableError(errorName);
        }
    }

    private void startServer() {

        BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                v(TAG, "onConnectionStateChange address: %s, status: %s, newState: %s", device.getAddress(), BtConst.nameGattStatus(status), BtConst.nameConnectionState(newState));
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                mHandler.post(() -> {

                    try {

                        String propValue = mAdvertisementSet.getProp(characteristic.getUuid());
                        v(TAG, "onCharacteristicReadRequest address: %s, requestId: %d, offset: %d, characteristic: %s, value: %s", device.getAddress(), requestId, offset, characteristic.getUuid(), propValue);

                        byte[] response = chunk(
                                propValue == null
                                        ? new byte[0]
                                        : propValue.getBytes(Charset.forName("UTF-8")),
                                offset);
                        v(TAG, "sending response, bytes: %d", response.length);
                        mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response);

                    } catch (UnknownAdvertisementException e) {
                        // Invalid characteristic
                        w(TAG, "Invalid characteristic requested, address: %s, characteristic: %s", device.getAddress(), characteristic.getUuid());
                        mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                    }
                });
            }
        };

        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);

        if (mBluetoothGattServer == null) {
            advertisingUnsupported(false, "could not start gatt server");

        } else {
            mBluetoothGattServer.addService(createSloganService());
        }
    }

    /**
     * onCharacteristicReadRequest is called until no more response is transmitted.
     * For that to be achieved sendResponse() has to be eventually invoked with an empty byte[].
     * Otherwise the payload is transmitted multiple times, significantly reducing throughput.
     */
    private byte[] chunk(byte[] source, int offset) {
        if (offset > source.length) {
            return new byte[0];
        }
        return Arrays.copyOfRange(source, offset, source.length);
    }

    private BluetoothGattService createSloganService() {

        BluetoothGattService service = new BluetoothGattService(UuidSet.SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        for (UUID uuid : AdvertisementSet.ADVERTISED_UUIDS) {
            BluetoothGattCharacteristic chara = new BluetoothGattCharacteristic(uuid, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
            if (!service.addCharacteristic(chara)) {
                e(TAG, "Could not add characteristic");
                mOnErrorCallback.onUnrecoverableError("Could not add characteristic");
            }
        }
        return service;
    }
}
