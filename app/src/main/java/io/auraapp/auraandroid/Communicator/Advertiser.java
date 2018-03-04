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

import java.util.Arrays;
import java.util.UUID;

import io.auraapp.auraandroid.common.Timer;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
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
    // TODO keep stats on how often a slogan has been received (max 100)
    // TODO keep stats on how often a slogan has been adopted (max 100)
    // TODO do both using separate simple stats/metering module using slogan hashes, not on slogan object itself

    @FunctionalInterface
    interface StateChangeCallback {
        void onStateChange(byte version, int id);
    }

    private final static String TAG = "@aura/ble/advertiser";
    private static final long ID_SHUFFLE_INTERVAL = 60 * 60 * 1000;

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
            mHandler.post(() -> advertisingUnsupported(false));
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

    void start() {
        mHandler.post(() -> {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                advertisingUnsupported(true);
                return;
            }
            mBluetoothAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

            if (mBluetoothAdvertiser == null) {
                advertisingUnsupported(true);
                return;
            }

            shuffleId();
            advertise();
            startServer();
            mTimer.setSerializedInterval("shuffle-id", () -> {
                shuffleId();
                mBluetoothAdvertiser.stopAdvertising(mAdvertisingCallback);
                advertise();
            }, ID_SHUFFLE_INTERVAL);
        });
    }

    void increaseVersion() {
        mAdvertisementSet.increaseVersion();
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


        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addServiceUuid(UuidSet.SERVICE_PARCEL)
                .addServiceData(UuidSet.SERVICE_DATA_PARCEL, mAdvertisementSet.getAdditionalData())
                .build();

        mBluetoothAdvertiser.startAdvertising(settings, data, mAdvertisingCallback);
        i(TAG, "started advertising, id: %d, version: %d, service: %s", mAdvertisementSet.mId, mAdvertisementSet.mVersion, UuidSet.SERVICE);
    }

    void stop() {
        mHandler.post(() -> {
            d(TAG, "Making sure advertising is stopped");
            mTimer.clear("shuffle-id");
            if (mBluetoothGattServer != null) {
                mBluetoothGattServer.clearServices();
                mBluetoothGattServer.close();
            }
            if (mBluetoothAdvertiser != null) {
                mBluetoothAdvertiser.stopAdvertising(mAdvertisingCallback);
            }
        });
    }

    private void advertisingUnsupported(boolean recoverable) {
        d(TAG, "Advertising seems to be unsupported on this device");
        stop();
        if (!recoverable) {
            mOnErrorCallback.onUnrecoverableError();
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
                    v(TAG, "onCharacteristicReadRequest address: %s, requestId: %d, offset: %d, characteristic: %s", device.getAddress(), requestId, offset, characteristic.getUuid());

                    try {
                        byte[] response = chunk(
                                mAdvertisementSet.getChunkedResponsePayload(characteristic.getUuid()),
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
            advertisingUnsupported(false);

        } else {
            mBluetoothGattServer.addService(createSloganService());
        }
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

        BluetoothGattService service = new BluetoothGattService(UuidSet.SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        for (UUID uuid : AdvertisementSet.ADVERTISED_UUIDS) {
            BluetoothGattCharacteristic chara = new BluetoothGattCharacteristic(uuid, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
            if (!service.addCharacteristic(chara)) {
                e(TAG, "Could not add characteristic");
                mOnErrorCallback.onUnrecoverableError();
            }
        }
        return service;
    }
}
