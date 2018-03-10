package io.auraapp.auraandroid.Communicator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.auraapp.auraandroid.common.Peer;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
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
class Scanner {


    @FunctionalInterface
    interface CurrentPeersCallback {
        void currentPeers(Set<Peer> peers);
    }

    private final static String TAG = "@aura/ble/scanner";

    private static final boolean HIGH_POWER = true;
    private static final long PEER_FORGET_AFTER = 1000 * 60 * 30;
    private static final long PEER_CONNECT_TIMEOUT = 1000 * 10;
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private final Communicator.OnErrorCallback mOnErrorCallback;

    private final Context mContext;
    private final Handler mHandler = new Handler();
    private final DeviceMap mDeviceMap = new DeviceMap();
    private final PeerBroadcaster mPeerBroadcaster;

    private boolean mQueued = false;
    private boolean mInactive = false;

    Scanner(Context context, PeerBroadcaster peerBroadcaster, Communicator.OnErrorCallback onErrorCallback) {
        mContext = context;
        mPeerBroadcaster = peerBroadcaster;
        mOnErrorCallback = onErrorCallback;
    }

    void start() {
        mHandler.post(() -> {
            mQueued = false;
            mInactive = false;
            startScanning();
            returnControl();
        });
    }

    private void returnControl() {
        if (mQueued || mInactive) {
            return;
        }
        mHandler.postDelayed(this::actOnState, 300);
        mQueued = true;
    }

    void stop() {
        mHandler.post(() -> {
            w(TAG, "onStop called, destroying");
            mInactive = true;
            for (Device device : mDeviceMap.values()) {
                disconnectDevice(device);
            }
            // Propagate peers after all devices disconnected
            mPeerBroadcaster.propagatePeerList(mDeviceMap);
        });
    }

    /**
     * Avoids concurrent access to mPeers
     */
    void requestPeers(CurrentPeersCallback currentPeersCallback) {
        mHandler.post(() -> currentPeersCallback.currentPeers(mPeerBroadcaster.buildPeers(mDeviceMap)));
    }

    private void disconnectDevice(Device device) {
        i(TAG, "Disconnecting device, slogans known: %d, id: %s, stats: %s", device.getSlogans().size(), device.mId, device.stats);
        if (device.bt.gatt != null) {
            device.bt.gatt.close();
        }
        device.bt.device = null;
        device.bt.gatt = null;
        device.bt.service = null;
        device.connected = false;
        device.shouldDisconnect = false;
        device.lastConnectAttempt = 0;
        device.isDiscoveringServices = false;
        device.mFetchingAProp = false;
        device.mSynchronizing = false;
    }

    private void actOnState() {

        if (mInactive) {
            // invocation of this method is posted on mHandler and the scanner could've been stopped since
            return;
        }

        mQueued = false;

        long now = System.currentTimeMillis();

        for (String id : mDeviceMap.ids()) {

            Device device = mDeviceMap.getById(id);

            try {
                if (device.shouldDisconnect) {
                    disconnectDevice(device);
                    // Giving the BT some air before we do the next connection attempt
                    continue;
                }

                if (!device.connected) {
                    if (device.bt.device == null) {
                        v(TAG, "Waiting for next time sight, id: %s", id);

                    } else if (device.lastConnectAttempt != 0 && now - device.lastConnectAttempt <= PEER_CONNECT_TIMEOUT) {
                        v(TAG, "Nothing to do, connection attempt is in progress, id: %s", id);

                    } else if (device.lastConnectAttempt != 0 && now - device.lastConnectAttempt > PEER_CONNECT_TIMEOUT) {
                        d(TAG, "Connection timeout, closing gatt, id: %s", id);
                        device.shouldDisconnect = true;
                        device.stats.mErrors++;

                    } else if (now - device.lastSeenTimestamp > PEER_FORGET_AFTER) {
                        // lastSeenTimestamp may be 0
                        v(TAG, "Forgetting device, id: %s", id);
                        device.bt.device = null;
                        device.bt.gatt = null;
                        device.bt.service = null;
                        mHandler.post(() -> {
                            mDeviceMap.removeById(id);
                            mPeerBroadcaster.propagatePeerList(mDeviceMap);
                        });

                    } else if (device.mOutdated) {
                        d(TAG, "Connecting to gatt server, id: %s", id);
                        device.connectionAttempts++;
                        device.mSynchronizing = true;
                        device.lastConnectAttempt = now;
                        device.setAllPropertiesOutdated();
                        if (device.bt.device == null) {
                            i(TAG, "BluetoothDevice is null, maybe BT has just been disabled, id: %s", id);
                            device.shouldDisconnect = true;
                            device.stats.mErrors++;
                            continue;
                        }
                        device.bt.gatt = device.bt.device.connectGatt(mContext, false, mGattCallback);

                    } else {
                        v(TAG, "Nothing to do for disconnected device, id: %s", id);
                    }
                    continue;
                }

                // device is currently connected

                if (device.bt.service == null && !device.isDiscoveringServices) {
                    device.isDiscoveringServices = true;
                    i(TAG, "Connected to %s, discovering services", id);
                    if (device.bt.gatt == null) {
                        device.stats.mErrors++;
                        device.shouldDisconnect = true;
                    }
                    device.bt.gatt.discoverServices();
                    continue;
                }
                if (device.bt.service == null) {
                    v(TAG, "Still discovering services, id: %s", id);
                    continue;
                }

                // device has a service and is not discovering

                if (device.mFetchingAProp) {
                    v(TAG, "Still fetching prop, id: %s", id);
                    continue;
                }

                UUID nextOutdatedCharaUuid = device.getFirstOutdatedPropertyUuid();

                if (nextOutdatedCharaUuid != null) {
                    requestCharacteristic(device, nextOutdatedCharaUuid);
                    continue;
                }

                i(TAG, "All props fresh, should disconnect, props: %s, id: %s", device.props(), id);
                device.mOutdated = false;
                device.mSynchronizing = false;
                device.stats.mSuccessfulRetrievals++;
                device.shouldDisconnect = true;
                mPeerBroadcaster.propagatePeer(device);
            } catch (Exception e) {
                e(TAG, "Unhandled exception, device: %s", device);
                throw e;
            }
        }
        returnControl();
    }

    private void requestCharacteristic(Device device, UUID uuid) {
        device.mFetchingAProp = true;
        d(TAG, "Requesting characteristic, device: %s, characteristic: %s", device.mId, uuid);
        BluetoothGattCharacteristic chara = device.bt.service.getCharacteristic(uuid);
        if (chara == null) {
            w(TAG, "Remote seems to not advertise characteristic. Disconnecting, id: %s, characteristic: %s", device.mId, uuid);
            device.shouldDisconnect = true;
            device.stats.mErrors++;
            returnControl();
            return;
        }
        if (!device.bt.gatt.readCharacteristic(chara)) {
            d(TAG, "Failed to request prop. Disconnecting, device: %s, characteristic: %s", device.mId, uuid);
            device.shouldDisconnect = true;
            device.stats.mErrors++;
            returnControl();
        }
    }

    private void startScanning() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            d(TAG, "Bluetooth is currently unavailable");
            stop();
            return;
        }
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        if (scanner == null) {
            d(TAG, "Bluetooth is currently unavailable");
            stop();
            return;
        }

        ScanSettings.Builder builder = new ScanSettings.Builder()
                .setScanMode(HIGH_POWER
                        ? ScanSettings.SCAN_MODE_LOW_LATENCY
                        : ScanSettings.SCAN_MODE_BALANCED);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            builder.setMatchMode(HIGH_POWER
                    ? ScanSettings.MATCH_MODE_AGGRESSIVE
                    : ScanSettings.MATCH_MODE_STICKY);
        }
        ScanSettings settings = builder.build();

        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder()
                .setServiceUuid(UuidSet.SERVICE_PARCEL)
                .build());

        scanner.startScan(scanFilters, settings, new ScanCallback() {

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                mHandler.post(() -> handleResults(new ScanResult[]{result}));
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                v(TAG, "onBatchScanResults callbackType: %d, result: %s", results == null ? "null" : results.toString());
                if (results != null) {
                    mHandler.post(() -> handleResults((ScanResult[]) results.toArray()));
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                w(TAG, "onScanFailed errorCode: %s", BtConst.nameScanError(errorCode));
                mHandler.post(() -> mOnErrorCallback.onUnrecoverableError(BtConst.nameScanError(errorCode)));
            }
        });
        i(TAG, "started scanning");
    }

    private boolean assertPeer(String address, BluetoothGatt gatt, String operation) {
        if (mDeviceMap.has(address)) {
            return true;
        }
        d(TAG, "No peer available for connection change (probably already forgotten). Closing gatt, operation: %s, address: %s", operation, address);
        gatt.close();
        return false;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            mHandler.post(() -> {
                String address = gatt.getDevice().getAddress();
                d(TAG, "onConnectionStateChange, device: %s, status: %s, newState: %s", address, BtConst.nameGattStatus(status), BtConst.nameConnectionState(newState));
                if (!assertPeer(address, gatt, "onConnectionStateChange")) {
                    return;
                }
                Device device = mDeviceMap.get(address);
                if (newState == STATE_CONNECTED) {
                    device.connected = true;
                    returnControl();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    device.connected = false;
                    d(TAG, "Disconnected from %s", device.mId);
                    returnControl();
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mHandler.post(() -> {
                String address = gatt.getDevice().getAddress();
                v(TAG, "onServicesDiscovered, device: %s, status: %s", address, BtConst.nameGattStatus(status));

                if (!assertPeer(address, gatt, "onServicesDiscovered")) {
                    return;
                }
                Device device = mDeviceMap.get(address);

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    w(TAG, "onServicesDiscovered unsuccessful, status: %s", BtConst.nameGattStatus(status));
                    device.shouldDisconnect = true;
                    device.stats.mErrors++;
                    returnControl();
                    return;
                }

                d(TAG, "Discovered %d services, id: %s, services: %s", gatt.getServices().size(), device.mId, gatt.getServices().toString());

                BluetoothGattService service = gatt.getService(UuidSet.SERVICE);

                if (service == null) {
                    i(TAG, "Service %s not advertised, disconnecting, id: %s", UuidSet.SERVICE, device.mId);
                    device.shouldDisconnect = true;
                    device.stats.mErrors++;
                    returnControl();
                    return;
                }
                device.isDiscoveringServices = false;
                device.bt.service = service;
                returnControl();
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            mHandler.post(() -> {
                String address = gatt.getDevice().getAddress();
                v(TAG, "onCharacteristicRead, address: %s, characteristic: %s, status: %s", address, characteristic.getUuid(), BtConst.nameGattStatus(status));

                if (!assertPeer(address, gatt, "onCharacteristicRead")) {
                    return;
                }

                Device device = mDeviceMap.get(address);

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    w(TAG, "onCharacteristicRead unsuccessful");
                    device.shouldDisconnect = true;
                    device.stats.mErrors++;
                    returnControl();
                    return;
                }
                byte[] value = characteristic.getValue();
                if (value == null) {
                    w(TAG, "Retrieved null prop, id: %s", device.mId);
                    device.shouldDisconnect = true;
                    device.stats.mErrors++;
                    returnControl();
                    return;
                }

                UUID uuid = characteristic.getUuid();
                String propValue = new String(value, UTF8_CHARSET);
                d(TAG, "Retrieved prop, id: %s, uuid: %s, propValue: %s", device.mId, uuid, propValue);
                try {
                    if (device.updateWithReceivedAttribute(uuid, propValue)) {
                        mPeerBroadcaster.propagatePeer(device);
                    }
                } catch (UnknownAdvertisementException e) {
                    w(TAG, "Characteristic retrieved matches no prop UUID, id: %s, uuid: %s", device.mId, uuid);
                }
                device.mFetchingAProp = false;
                returnControl();
            });
        }
    };

    private void handleResults(ScanResult[] results) {
        if (mInactive) {
            // invocation of this method is posted on mHandler and the scanner could've been stopped since
            return;
        }
        for (ScanResult result : results) {

            String address = result.getDevice().getAddress();

            AdditionalDataUnpacker.Result additionalData = AdditionalDataUnpacker.unpack(result.getScanRecord());

            final String id = additionalData.isPresent()
                    ? "" + additionalData.getId()
                    : address;

            mDeviceMap.setId(address, id);

            final Device device = mDeviceMap.get(address);
            if (device != null) {
                if (!result.getDevice().equals(device.bt.device)) {
                    // This is the case if the advertisement is restarted, e.g. in case the version changes
                    i(TAG, "Resetting remote device instance, BT address changed since last seen, id: %s", id);
                    device.bt.device = result.getDevice();
                }
                v(TAG, "Known device seen, id: %s", id);
                device.lastSeenTimestamp = System.currentTimeMillis();
                if (additionalData.isPresent() && device.mAdvertisementVersion != additionalData.getVersion()) {
                    device.mOutdated = true;
                    device.mAdvertisementVersion = additionalData.getVersion();
                }
                mPeerBroadcaster.propagatePeer(device);
                return;
            }
            i(TAG, "Device yet unknown, id: %s", id);
            final Device unknownDevice = Device.create(id, result.getDevice());
            unknownDevice.lastSeenTimestamp = System.currentTimeMillis();
            mDeviceMap.put(address, unknownDevice);
            mPeerBroadcaster.propagatePeerList(mDeviceMap);
        }
    }
}
