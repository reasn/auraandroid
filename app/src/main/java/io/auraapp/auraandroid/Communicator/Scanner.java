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
import android.os.ParcelUuid;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.auraapp.auraandroid.common.CuteHasher;
import io.auraapp.auraandroid.common.Peer;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE;
import static android.bluetooth.le.ScanSettings.MATCH_MODE_STICKY;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;
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
    private static final long PEER_REFRESH_AFTER = 1000 * 20;
    private static final long PEER_CONNECT_TIMEOUT = 1000 * 10;
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private final Context mContext;
    private final Handler mHandler = new Handler();
    /**
     * Uses the address as index. Android doesn't return true device addresses for privacy reasons.
     */
    private final HashMap<String, Device> mDeviceMap = new HashMap<>();
    private final PeerBroadcaster mPeerBroadcaster;

    private boolean mQueued = false;
    private boolean mInactive = false;

    Scanner(Context context, PeerBroadcaster peerBroadcaster) {
        mContext = context;
        mPeerBroadcaster = peerBroadcaster;
    }

    void start() {
        mHandler.post(() -> {
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
            for (String address : mDeviceMap.keySet()) {

                Device device = mDeviceMap.get(address);
                if (device.bt.gatt != null) {
                    device.bt.gatt.close();
                }
                device.bt.device = null;
                device.bt.service = null;
            }
            mHandler.post(mDeviceMap::clear);
        });
    }

    /**
     * Avoids concurrent access to mPeers
     */
    void requestPeers(CurrentPeersCallback currentPeersCallback) {
        mHandler.post(() -> currentPeersCallback.currentPeers(mPeerBroadcaster.buildPeers(mDeviceMap)));
    }

    private void actOnState() {
        mQueued = false;

        long now = System.currentTimeMillis();

        for (String address : mDeviceMap.keySet()) {

            Device device = mDeviceMap.get(address);
            String addressHash = CuteHasher.hash(address);

            try {
                if (device.shouldDisconnect) {
                    i(TAG, "Disconnecting device, slogans known: %d, device: %s, stats: %s", device.getSlogans().size(), addressHash, device.stats);
                    if (device.bt.gatt != null) {
                        device.bt.gatt.close();
                    }
                    device.bt.gatt = null;
                    device.bt.service = null;
                    device.connected = false;
                    device.shouldDisconnect = false;
                    device.lastConnectAttempt = 0;
                    device.isDiscoveringServices = false;
                    device.isFetchingProp = false;

                    // Giving the BT some air before we do the next connection attempt
                    continue;
                }

                if (!device.connected) {
                    if (device.lastConnectAttempt != 0 && now - device.lastConnectAttempt <= PEER_CONNECT_TIMEOUT) {
//                        v(TAG, "Nothing to do, connection attempt is in progress, addressHash: %s", addressHash);

                    } else if (device.lastConnectAttempt != 0 && now - device.lastConnectAttempt > PEER_CONNECT_TIMEOUT) {
                        d(TAG, "Connection timeout, closing gatt, addressHash: %s", addressHash);
                        device.shouldDisconnect = true;
                        device.stats.mErrors++;

                    } else if (now - device.lastSeenTimestamp > PEER_FORGET_AFTER) {
                        // lastSeenTimestamp may be 0
                        v(TAG, "Forgetting device, addressHash: %s", addressHash);
                        device.bt.device = null;
                        mHandler.post(() -> {
                            mDeviceMap.remove(address);
                            mPeerBroadcaster.propagatePeerList(mDeviceMap);
                        });

                    } else if (device.mNextFetch == 0 || device.mNextFetch < now) {
                        d(TAG, "Connecting to gatt server, addressHash: %s", addressHash);
                        device.connectionAttempts++;
                        device.lastConnectAttempt = now;
                        device.setAllPropertiesOutdated();
                        device.bt.gatt = device.bt.device.connectGatt(mContext, false, mGattCallback);

                    } else {
//                        v(TAG, "Nothing to do for disconnected device, addressHash: %s", addressHash);
                    }
                    continue;
                }

                // device is currently connected

                if (device.bt.service == null && !device.isDiscoveringServices) {
                    device.isDiscoveringServices = true;
                    i(TAG, "Connected to %s, discovering services", addressHash);
                    device.bt.gatt.discoverServices();
                    continue;
                }
                if (device.bt.service == null) {
                    v(TAG, "Still discovering services, device: %s", addressHash);
                    continue;
                }

                // device has a service and is not discovering

                if (device.isFetchingProp) {
                    v(TAG, "Still fetching prop, addressHash: %s", addressHash);
                    continue;
                }

                UUID nextOutdatedCharaUuid = device.getFirstOutdatedPropertyUuid();

                if (nextOutdatedCharaUuid != null) {
                    requestCharacteristic(device, nextOutdatedCharaUuid);
                    continue;
                }

                device.mNextFetch = now + PEER_REFRESH_AFTER;
                i(TAG, "All props fresh, should disconnect, nextFetch: %d, props: %s, addressHash: %s", device.mNextFetch, device.props(), addressHash);
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
        device.isFetchingProp = true;
        String addressHash = CuteHasher.hash(device.bt.device.getAddress());
        d(TAG, "Requesting characteristic, gatt: %s, characteristic: %s", addressHash, uuid);
        BluetoothGattCharacteristic chara = device.bt.service.getCharacteristic(uuid);
        if (chara == null) {
            w(TAG, "Remote seems to not advertise characteristic. Disconnecting, addressHash: %s, characteristic: %s", addressHash, uuid);
            device.shouldDisconnect = true;
            device.stats.mErrors++;
            returnControl();
            return;
        }
        if (!device.bt.gatt.readCharacteristic(chara)) {
            d(TAG, "Failed to request prop. Disconnecting, gatt: %s, characteristic: %s", addressHash, uuid);
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
                        ? SCAN_MODE_LOW_LATENCY
                        : SCAN_MODE_BALANCED);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            builder.setCallbackType(CALLBACK_TYPE_ALL_MATCHES);
            builder.setMatchMode(HIGH_POWER
                    ? MATCH_MODE_AGGRESSIVE
                    : MATCH_MODE_STICKY);
        }
        ScanSettings settings = builder.build();

        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UuidSet.SERVICE))
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
                // TODO disable scanning or at least stop
            }
        });
        i(TAG, "started scanning");
    }

    private boolean assertPeer(String address, BluetoothGatt gatt, String operation) {
        if (mDeviceMap.containsKey(address)) {
            return true;
        }
        d(TAG, "No peer available for connection change (probably already forgotten), operation: %s, address: %s", operation, address);

        // TODO can this throw errors? Are they safe to be ignored?;
        gatt.close();

        return false;
    }

    // TODO extract
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            mHandler.post(() -> {
                String address = gatt.getDevice().getAddress();
                String addressHash = CuteHasher.hash(address);
                d(TAG, "onConnectionStateChange, gatt: %s, status: %s, newState: %s", addressHash, BtConst.nameGattStatus(status), BtConst.nameConnectionState(newState));
                if (!assertPeer(address, gatt, "onConnectionStateChange")) {
                    return;
                }

                if (newState == STATE_CONNECTED) {
                    mDeviceMap.get(address).connected = true;
                    returnControl();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    d(TAG, "Disconnected from %s", addressHash);
                    mDeviceMap.get(address).connected = false;
                    returnControl();
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mHandler.post(() -> {
                String address = gatt.getDevice().getAddress();
                String addressHash = CuteHasher.hash(address);
                v(TAG, "onServicesDiscovered, gatt: %s, status: %s", addressHash, BtConst.nameGattStatus(status));

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

                d(TAG, "Discovered %d services, gatt: %s, services: %s", gatt.getServices().size(), addressHash, gatt.getServices().toString());

                BluetoothGattService service = gatt.getService(UuidSet.SERVICE);

                if (service == null) {
                    i(TAG, "Service %s not advertised, disconnecting, addressHash: %s", UuidSet.SERVICE, addressHash);
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
                String addressHash = CuteHasher.hash(address);
                v(TAG, "onCharacteristicRead, gatt: %s, characteristic: %s, status: %s", addressHash, characteristic.getUuid(), BtConst.nameGattStatus(status));

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
                    w(TAG, "Retrieved null prop, addressHash: %s", addressHash);
                    device.shouldDisconnect = true;
                    device.stats.mErrors++;
                    returnControl();
                    return;
                }

                UUID uuid = characteristic.getUuid();
                String propValue = new String(value, UTF8_CHARSET);
                d(TAG, "Retrieved prop, addressHash: %s, uuid: %s, propValue: %s", addressHash, uuid, propValue);
                try {
                    if (device.updateWithReceivedAttribute(uuid, propValue)) {
                        mPeerBroadcaster.propagatePeer(device);
                    }
                } catch (UnknownAdvertisementException e) {
                    w(TAG, "Characteristic retrieved matches no prop UUID, address: %s, uuid: %s", address, uuid);
                }
                device.isFetchingProp = false;
                returnControl();
            });
        }
    };

    private void handleResults(ScanResult[] results) {
        for (ScanResult result : results) {

            final String address = result.getDevice().getAddress();
            final Device device = mDeviceMap.get(address);
            if (device != null) {
                v(TAG, "Nothing to do, device already known, addressHash: %s", CuteHasher.hash(address));
                device.lastSeenTimestamp = System.currentTimeMillis();
                mPeerBroadcaster.propagatePeer(device);
            } else {
                i(TAG, "Device yet unknown, addressHash: %s", CuteHasher.hash(address));
                mHandler.post(() -> {
                    final Device unknownDevice = Device.create(result.getDevice());
                    unknownDevice.lastSeenTimestamp = System.currentTimeMillis();
                    mDeviceMap.put(address, unknownDevice);
                    mPeerBroadcaster.propagatePeerList(mDeviceMap);
                });
            }
        }
    }
}
