package io.auraapp.auranative22.Communicator;

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
import android.util.Log;

import java.nio.charset.Charset;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.e;
import static io.auraapp.auranative22.FormattedLog.i;
import static io.auraapp.auranative22.FormattedLog.v;

public class Advertiser {

    private final static String TAG = "@aura/ble/advertiser";
    private final UUID mSlogan3Uuid;
    private final UUID mSlogan1Uuid;
    private final UUID mSlogan2Uuid;
    private final UUID mServiceUuid;
    private final byte[] mSlogan1;
    private final byte[] mSlogan2;
    private final byte[] mSlogan3;

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private Context mContext;

    Advertiser(BluetoothManager bluetoothManager,
               UUID serviceUuid,
               UUID slogan1Uuid,
               UUID slogan2Uuid,
               UUID slogan3Uuid,
               String slogan1,
               String slogan2,
               String slogan3,
               Context context) {
        mBluetoothManager = bluetoothManager;
        mServiceUuid = serviceUuid;
        mSlogan1Uuid = slogan1Uuid;
        mSlogan2Uuid = slogan2Uuid;
        mSlogan3Uuid = slogan3Uuid;
        mContext = context;

        mSlogan1 = slogan1.getBytes(Charset.forName("UTF-8"));
        mSlogan2 = slogan2.getBytes(Charset.forName("UTF-8"));
        mSlogan3 = slogan3.getBytes(Charset.forName("UTF-8"));
    }

    void start() {
        advertise();
        startServer();
    }

    void stop() {
        // TODO
    }

    private void startServer() {

        BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                v(TAG, "onConnectionStateChange device: %s, status: %s, newState: %s", device.getAddress(), BtConst.nameStatus(status), BtConst.nameConnectionState(newState));
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

                d(TAG, "onCharacteristicReadRequest device: %s, requestId: %d, offset: %d, characteristic: %s", device.getAddress(), requestId, offset, characteristic.getUuid());

                if (mSlogan1Uuid.equals(characteristic.getUuid())) {
                    i(TAG, "sending slogan 1");
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, mSlogan1);
                } else if (mSlogan2Uuid.equals(characteristic.getUuid())) {
                    i(TAG, "sending slogan 2");
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, mSlogan2);
                } else if (mSlogan3Uuid.equals(characteristic.getUuid())) {
                    i(TAG, "sending slogan 3");
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, mSlogan3);
                } else {
                    // Invalid characteristic
                    Log.w("ble", "Invalid Characteristic Read: " + characteristic.getUuid());
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        };

        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);

        assert mBluetoothGattServer != null;

        mBluetoothGattServer.addService(createSloganService());
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
                advertisingUnsupported();
            }
        }
        return service;
    }

    /**
     * Inspiration
     * - https://code.tutsplus.com/tutorials/how-to-advertise-android-as-a-bluetooth-le-peripheral--cms-25426
     */
    private void advertise() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                // Disable timeout for advertising
                .setTimeout(0)
                .build();

//        bluetoothAdapter.setName("#");
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(mServiceUuid))
                .build();

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                d(TAG, "onStartSuccess, settings: %s", settingsInEffect);
//                Toast.makeText(context, "Advertising", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartFailure(int errorCode) {
                e(TAG, "onStartFailure, errorCode: %s", BtConst.nameAdvertiseError(errorCode));
                advertisingUnsupported();
            }
        };

        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            advertisingUnsupported();
        } else {
            advertiser.startAdvertising(settings, data, advertisingCallback);
            i(TAG, "started advertising");
        }
    }

    private void advertisingUnsupported() {

    }
}
