package io.auraapp.auranative22;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.e;
import static io.auraapp.auranative22.FormattedLog.i;

public class AdvertiseService extends Service {

    private final static String TAG = "@aura/ble";

    public static final int FOREGROUND_ID = 1337;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        final Notification notification = new Notification.Builder(this)
                .setContentTitle("ContentTitle")
                .setContentText("ContentTExt")
                .setContentIntent(pendingIntent)
                .setTicker("ticker")
                .build();

        startForeground(FOREGROUND_ID, notification);

        Context context = getApplicationContext();

        advertise();
        startServer();
        Toast.makeText(context, "advertising", Toast.LENGTH_SHORT).show();

        return START_STICKY;
    }

    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    private void startServer() {


        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert mBluetoothManager != null;


        UUID serviceUuid = UUID.fromString(getString(R.string.ble_uuid_service));
        final UUID slogan1Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_1));
        final UUID slogan2Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_2));
        final UUID slogan3Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_3));

        final byte[] slogan1 = "Helloo World, Hello!1Helloo World, 🚀 Hello!1Helloo World, Hello!1Helloo World, H".getBytes(Charset.forName("UTF-8"));
        final byte[] slogan2 = "Fappoo LorpdFappo!1FappoLorpdFappo!1FappoLorpdFappo!1FappoLorpdF nanunana wadatap".getBytes(Charset.forName("UTF-8"));
        final byte[] slogan3 = "The lazy chicken jumps over the running dog on its way to the alphabet. Cthullu !".getBytes(Charset.forName("UTF-8"));


        BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                d(TAG, "onConnectionStateChange devce: %s, status: %d, newState: %d", device.getAddress(), status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    i(TAG, "BluetoothDevice CONNECTED: " + device);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                    //Remove device from any active subscriptions
                    mRegisteredDevices.remove(device);
                }
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

                d(TAG, "onCharacteristicReadRequest device: %s, requestId: %d, offset: %d, characteristic: %s", device.getAddress(), requestId, offset, characteristic.getUuid());

                if (slogan1Uuid.equals(characteristic.getUuid())) {
                    i(TAG, "sending slogan 1");
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, slogan1);
                } else if (slogan2Uuid.equals(characteristic.getUuid())) {
                    i(TAG, "sending slogan 2");
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, slogan2);
                } else if (slogan3Uuid.equals(characteristic.getUuid())) {
                    i(TAG, "sending slogan 3");
                    mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, slogan3);
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

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                if (2 > 1) {
                    throw new RuntimeException("Not implemented");
                }
//                if (TimeProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
//                    Log.d("ble", "Config descriptor read");
//                    byte[] returnValue;
//                    if (mRegisteredDevices.contains(device)) {
//                        returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
//                    } else {
//                        returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
//                    }
//                    mBluetoothGattServer.sendResponse(device,
//                            requestId,
//                            BluetoothGatt.GATT_FAILURE,
//                            0,
//                            returnValue);
//                } else {
//                    Log.w("ble", "Unknown descriptor read request");
//                    mBluetoothGattServer.sendResponse(device,
//                            requestId,
//                            BluetoothGatt.GATT_FAILURE,
//                            0,
//                            null);
//                }
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattDescriptor descriptor,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
                if (2 > 1) {
                    throw new RuntimeException("Not implemented");
                }
//                if (TimeProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
//                    if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
//                        Log.d("ble", "Subscribe device to notifications: " + device);
//                        mRegisteredDevices.add(device);
//                    } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
//                        Log.d("ble", "Unsubscribe device from notifications: " + device);
//                        mRegisteredDevices.remove(device);
//                    }
//
//                    if (responseNeeded) {
//                        mBluetoothGattServer.sendResponse(device,
//                                requestId,
//                                BluetoothGatt.GATT_SUCCESS,
//                                0,
//                                null);
//                    }
//                } else {
//                    Log.w("ble", "Unknown descriptor write request");
//                    if (responseNeeded) {
//                        mBluetoothGattServer.sendResponse(device,
//                                requestId,
//                                BluetoothGatt.GATT_FAILURE,
//                                0,
//                                null);
//                    }
//                }
            }
        };

        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);

        assert mBluetoothGattServer != null;

        mBluetoothGattServer.addService(createSloganService());

//        BluetoothGattService timeService = TimeProfile.createTimeService();
//        mBluetoothGattServer.addService(timeService);
    }

    private BluetoothGattService createSloganService() {

        BluetoothGattService service = new BluetoothGattService(
                UUID.fromString(getString(R.string.ble_uuid_service)),
                BluetoothGattService.SERVICE_TYPE_PRIMARY
        );

        for (UUID uuid : new UUID[]{
                UUID.fromString(getString(R.string.ble_uuid_slogan_1)),
                UUID.fromString(getString(R.string.ble_uuid_slogan_2)),
                UUID.fromString(getString(R.string.ble_uuid_slogan_3)),
        }) {
            BluetoothGattCharacteristic chara = new BluetoothGattCharacteristic(uuid, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
            if (!service.addCharacteristic(chara)) {
                throw new RuntimeException("Could not add characteristic");
            }
        }
        return service;
    }


    /**
     * Sources and inspiration
     * - https://code.tutsplus.com/tutorials/how-to-advertise-android-as-a-bluetooth-le-peripheral--cms-25426
     */

    private void advertise() {

        final Context context = getApplicationContext();

        if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
            Toast.makeText(context, "Multiple advertisement not supported", Toast.LENGTH_SHORT).show();
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                // Disable timeout for advertising
                .setTimeout(0)
                .build();

        ParcelUuid pUuid = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid_service)));

//        bluetoothAdapter.setName("#");
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addServiceUuid(pUuid)
                .build();

        AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Toast.makeText(context, "Advertising", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartFailure(int errorCode) {
                // errorCode === 1 indicates that there's too much data to be advertised, https://github.com/PaulTR/BluetoohLEAdvertising/issues/1
                Toast.makeText(context, "Advertising onStartFailure: " + errorCode, Toast.LENGTH_LONG).show();
                e(TAG, "Advertising onStartFailure: " + errorCode);
            }
        };

        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            advertisingUnsupported();
        }
        advertiser.startAdvertising(settings, data, advertisingCallback);
        i(TAG, "started advertising");
    }

    private void advertisingUnsupported() {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
