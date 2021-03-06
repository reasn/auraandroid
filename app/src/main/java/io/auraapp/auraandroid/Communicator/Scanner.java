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

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.Peer;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static io.auraapp.auraandroid.Communicator.MetaDataUnpacker.byteArrayToString;
import static io.auraapp.auraandroid.common.Config.COMMUNICATOR_INCOMPLETE_PEER_FORGET_AFTER;
import static io.auraapp.auraandroid.common.Config.DEBUG_FAKE_PEER_CHARACTERISTIC_RETRIEVAL_FAILURE;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.iv;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;

/**
 * All methods accessible from the outside post a callback to mHandler to avoid
 * concurrent modification of any class properties.
 * The same holds for all callbacks registered externally (in this case typically the BT stack).
 */
class Scanner {


    private long mPeerRetention;

    @FunctionalInterface
    interface CurrentPeersCallback {
        void currentPeers(Set<Peer> peers);
    }

    private final static String TAG = "@aura/ble/scanner";

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private final Communicator.OnUnrecoverableBtErrorCallback mOnUnrecoverableBtErrorCallback;

    private final Context mContext;
    private final Handler mHandler = new Handler();
    private final DeviceMap mDeviceMap = new DeviceMap();
    private final PeerBroadcaster mPeerBroadcaster;

    private boolean mQueued = false;
    private boolean mInactive = false;

    Scanner(Context context, PeerBroadcaster peerBroadcaster, Communicator.OnUnrecoverableBtErrorCallback onUnrecoverableBtErrorCallback) {
        mContext = context;
        mPeerBroadcaster = peerBroadcaster;
        mOnUnrecoverableBtErrorCallback = onUnrecoverableBtErrorCallback;

        AuraPrefs.listen(mContext, R.string.prefs_retention_key, value -> {
            mPeerRetention = Long.parseLong((String) value);
            i(TAG, "Retention set to %d", mPeerRetention);
        });
        mPeerRetention = AuraPrefs.getPeerRetention(mContext);
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
            i(TAG, "onStop called, destroying");
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
        i(TAG, "Disconnecting device, slogans known: %d, id: %s, stats: %s",
                device.buildSlogans().size(),
                Integer.toHexString(device.mId),
                device.stats);
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
        device.clearDebouncer();
    }

    private void actOnState() {

        if (mInactive) {
            // invocation of this method is posted on mHandler and the scanner could've been stopped since
            return;
        }

        mQueued = false;

        long now = System.currentTimeMillis();
        for (int id : mDeviceMap.ids()) {

            Device device = mDeviceMap.getById(id);
            String hexId = Integer.toHexString(id);

            try {
                if (device.shouldDisconnect) {
                    disconnectDevice(device);
                    if (device.stats.mSuccessfulRetrievals == 0) {
                        mHandler.post(() -> {
                            mDeviceMap.removeById(id);
                            mPeerBroadcaster.propagatePeerList(mDeviceMap);
                        });
                        returnControl();
                        return;
                    } else {
                        mHandler.post(() -> mPeerBroadcaster.propagatePeer(device, false, countSlogans()));
                    }
                    // Giving the BT some air before we do the next connection attempt
                    continue;
                }

                if (!device.connected) {
                    if (device.lastConnectAttempt != 0 && now - device.lastConnectAttempt <= Config.COMMUNICATOR_PEER_CONNECT_TIMEOUT) {
                        v(TAG, "Nothing to do, connection attempt is in progress, id: %s", hexId);
                        continue;
                    }
                    if (device.lastConnectAttempt != 0 && now - device.lastConnectAttempt > Config.COMMUNICATOR_PEER_CONNECT_TIMEOUT) {
                        d(TAG, "Connection timeout, closing gatt, id: %s", hexId);
                        device.shouldDisconnect = true;
                        device.stats.mErrors++;
                        continue;
                    }

                    long ttl = device.stats.mSuccessfulRetrievals == 0
                            ? COMMUNICATOR_INCOMPLETE_PEER_FORGET_AFTER
                            : mPeerRetention;

                    if (now - device.lastSeenTimestamp > ttl) {
                        // lastSeenTimestamp may be 0
                        v(TAG, "Forgetting device, id: %s", hexId);
                        device.bt.device = null;
                        device.bt.gatt = null;
                        device.bt.service = null;
                        mHandler.post(() -> {
                            mDeviceMap.removeById(id);
                            mPeerBroadcaster.propagatePeerList(mDeviceMap);
                        });
                        returnControl();
                        return;
                    }

                    if (device.mOutdated && device.bt.device != null) {
                        d(TAG, "Device is marked as outdated, connecting to gatt server, id: %s", hexId);
                        device.connectionAttempts++;
                        device.mSynchronizing = true;
                        device.lastConnectAttempt = now;
                        device.setAllPropertiesOutdated();
                        if (device.bt.device == null) {
                            i(TAG, "BluetoothDevice is null, maybe BT has just been disabled, id: %s", hexId);
                            device.shouldDisconnect = true;
                            device.stats.mErrors++;
                            continue;
                        }
                        device.bt.gatt = device.bt.device.connectGatt(mContext, false, mGattCallback);
                        continue;
                    }

                    v(TAG, "Nothing to do for disconnected device, id: %s, will be forgotten in %ds, waiting for BT device: %b, lastConnectAttempt: %d, lastSeen: %d, successfulRetrievals: %d",
                            hexId,
                            (device.lastSeenTimestamp - now + ttl) / 1000,
                            device.bt.device == null,
                            device.lastConnectAttempt > 0 ? now - device.lastConnectAttempt : -1,
                            now - device.lastSeenTimestamp,
                            device.stats.mSuccessfulRetrievals
                    );

                    continue;
                }

                // device is currently connected

                if (device.bt.service == null && !device.isDiscoveringServices) {
                    device.isDiscoveringServices = true;
                    i(TAG, "Connected to %s, discovering services", hexId);
                    if (device.bt.gatt == null) {
                        device.stats.mErrors++;
                        device.shouldDisconnect = true;
                    }
                    device.bt.gatt.discoverServices();
                    continue;
                }
                if (device.bt.service == null) {
                    v(TAG, "Still discovering services, id: %s", hexId);
                    continue;
                }

                // device has a service and is not discovering

                if (device.mFetchingAProp) {
                    v(TAG, "Still fetching prop, id: %s", hexId);
                    continue;
                }

                UUID nextOutdatedCharaUuid = device.getFirstOutdatedPropertyUuid();

                if (DEBUG_FAKE_PEER_CHARACTERISTIC_RETRIEVAL_FAILURE) {
                    continue;
                }
                if (nextOutdatedCharaUuid != null) {
                    requestCharacteristic(device, nextOutdatedCharaUuid);
                    continue;
                }

                i(TAG, "All props fresh, should disconnect, props: %s, id: %s", device.props(), hexId);
                device.mOutdated = false;
                device.mSynchronizing = false;
                device.stats.mSuccessfulRetrievals++;
                device.shouldDisconnect = true;
                propagatePeerAndConsiderListUpdate(device, false, countSlogans());
            } catch (Exception e) {
                e(TAG, "Unhandled exception, device: %s", device);
                throw e;
            }
        }
        returnControl();
    }

    private int mLastPropagatedPeerCount = -1;

    /**
     * Propagates changes to a peer corresponding to `device`.
     * <p>
     * Also checks if the peer list has been propagated with the current number of peers.
     * If not, the full list is propagated.
     * Background: Without this method, a new peer would only be added if its advertisement were
     * observed after all characteristics have been received. If the peer went out of range in between,
     * it would remain invisible.
     */
    private void propagatePeerAndConsiderListUpdate(Device device, boolean contentChanged, int sloganCount) {

        if (mLastPropagatedPeerCount != mDeviceMap.ids().size()) {
            // `device` was not yet part of a `propagatePeerList` invocation
            mLastPropagatedPeerCount = mDeviceMap.ids().size();
            mPeerBroadcaster.propagatePeerList(mDeviceMap);
        } else {
            // `device` was already part of a `propagatePeerList` invocation, just update it
            mPeerBroadcaster.propagatePeer(device, contentChanged, sloganCount);
        }
    }

    private int countSlogans() {
        int sloganCount = 0;
        for (Device device : mDeviceMap.values()) {
            sloganCount += device.countSlogans();
        }
        return sloganCount;
    }

    private void requestCharacteristic(Device device, UUID uuid) {
        device.mFetchingAProp = true;
        d(TAG, "Requesting characteristic, id: %s, characteristic: %s", Integer.toHexString(device.mId), uuid);
        BluetoothGattCharacteristic chara = device.bt.service.getCharacteristic(uuid);
        if (chara == null) {
            w(TAG, "Remote seems to not advertise characteristic. Disconnecting, id: %s, characteristic: %s",
                    Integer.toHexString(device.mId),
                    uuid);
            device.shouldDisconnect = true;
            device.stats.mErrors++;
            returnControl();
            return;
        }
        if (!device.bt.gatt.readCharacteristic(chara)) {
            d(TAG, "Failed to request prop. Disconnecting, id: %s, characteristic: %s", Integer.toHexString(device.mId), uuid);
            device.shouldDisconnect = true;
            device.stats.mErrors++;
            returnControl();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void startScanning() {

        i(TAG, "Starting to scan, retention: %d", mPeerRetention);
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
                .setScanMode(Config.COMMUNICATOR_HIGH_POWER
                        ? ScanSettings.SCAN_MODE_LOW_LATENCY
                        : ScanSettings.SCAN_MODE_BALANCED);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            builder.setMatchMode(Config.COMMUNICATOR_HIGH_POWER
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
                mHandler.post(() -> mOnUnrecoverableBtErrorCallback.onUnrecoverableBtError(BtConst.nameScanError(errorCode)));
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
                d(TAG, "onConnectionStateChange, address: %s, status: %s, newState: %s", address, BtConst.nameGattStatus(status), BtConst.nameConnectionState(newState));
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
                v(TAG, "onServicesDiscovered, address: %s, status: %s", address, BtConst.nameGattStatus(status));

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

                d(TAG, "Discovered %d services, id: %s, services: %s",
                        gatt.getServices().size(),
                        Integer.toHexString(device.mId),
                        gatt.getServices().toString());

                BluetoothGattService service = gatt.getService(UuidSet.SERVICE);

                if (service == null) {
                    i(TAG, "Service %s not advertised, disconnecting, id: %s", UuidSet.SERVICE, Integer.toHexString(device.mId));
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
                v(TAG, "onCharacteristicRead, address: %s, characteristic: %s, status: %s",
                        address,
                        characteristic.getUuid(),
                        BtConst.nameGattStatus(status));

                if (!assertPeer(address, gatt, "onCharacteristicRead")) {
                    return;
                }

                Device device = mDeviceMap.get(address);
                String hexId = Integer.toHexString(device.mId);

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    i(TAG, "onCharacteristicRead unsuccessful");
                    device.shouldDisconnect = true;
                    device.stats.mErrors++;
                    returnControl();
                    return;
                }
                byte[] value = characteristic.getValue();
                if (value == null) {
                    w(TAG, "Retrieved null prop, id: %s", hexId);
                    device.shouldDisconnect = true;
                    device.stats.mErrors++;
                    returnControl();
                    return;
                }

                UUID uuid = characteristic.getUuid();
                String propValue = new String(value, UTF8_CHARSET);

                if (propValue.length() == 0) {
                    w(TAG, "Retrieved zero-length prop, id: %s, uuid: %s", hexId, uuid);
                } else {
                    d(TAG, "Retrieved prop, length: %d, id: %s, uuid: %s, propValue: %s",
                            propValue.length(),
                            hexId,
                            uuid,
                            propValue);
                }
                try {
                    DevicePeerProfile previousProfile = device.buildProfile();
                    List<String> previousSlogans = device.buildSlogans();
                    if (device.updateWithReceivedAttribute(uuid, propValue)) {

                        // Let's find out if any existing slogan changed or a slogan was added.
                        // If that's the case, contentAdded is true and will result in a notification
                        List<String> newSlogans = device.buildSlogans();
                        boolean existingSloganChanged = false;
                        for (String slogan : previousSlogans) {
                            if (!newSlogans.contains(slogan)) {
                                existingSloganChanged = true;
                                break;
                            }
                        }

                        DevicePeerProfile profile = device.buildProfile();
                        v(TAG, "Propagating peer updated with received characteristic, slogans: %s, slogan changed: %s, profile changed: %s",
                                newSlogans.size(),
                                existingSloganChanged,
                                !profile.equals(previousProfile)
                        );
                        // device changed, let's recheck if there's any duplicates
                        if (ScannerUtils.removeOldDevicesWithSameContent(mDeviceMap)) {
                            mPeerBroadcaster.propagatePeerList(mDeviceMap);
                        } else {
                            propagatePeerAndConsiderListUpdate(
                                    device,
                                    newSlogans.size() > previousSlogans.size()
                                            || existingSloganChanged
                                            || !profile.equals(previousProfile),
                                    countSlogans());
                        }
                    }
                } catch (UnknownAdvertisementException e) {
                    w(TAG, "Characteristic retrieved matches no prop UUID, id: %s, uuid: %s", hexId, uuid);
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

            MetaDataUnpacker.MetaData metaData = null;
            byte[] rawMeta = new byte[0];

            if (result.getScanRecord() == null) {
                w(TAG, "Meta data is null");
            } else {
                rawMeta = result.getScanRecord().getServiceData(UuidSet.SERVICE_DATA_PARCEL);
                metaData = MetaDataUnpacker.unpack(rawMeta);
            }

            final int id = metaData != null
                    ? metaData.getId()
                    : Integer.decode("0x" + address.replaceAll(":", ""));

            final String hexId = Integer.toHexString(id);

            mDeviceMap.setId(address, id);
            final Device device = mDeviceMap.get(address);
            if (device != null) {
                if (!result.getDevice().equals(device.bt.device)) {
                    // This is the case if the advertisement is restarted, e.g. in case the version changes
                    i(TAG, "Resetting remote device instance, BT address changed since last seen, id: %s", hexId);
                    device.bt.device = result.getDevice();
                }

                iv(TAG, "Seen known device, id: %s, connected: %s, version: %d (was %d), meta: %s, unpacked: %s",
                        hexId,
                        device.connected,
                        metaData != null
                                ? metaData.getDataVersion()
                                : "null",
                        device.mDataVersion,
                        byteArrayToString(rawMeta),
                        metaData);

                device.lastSeenTimestamp = System.currentTimeMillis();
                if (metaData != null && device.mDataVersion != metaData.getDataVersion()) {
                    device.mOutdated = true;
                    device.mDataVersion = metaData.getDataVersion();
                }
                propagatePeerAndConsiderListUpdate(device, false, countSlogans());
                return;
            }
            final Device unknownDevice = Device.create(id, result.getDevice());
            i(TAG, "Seen unknown device, id: %s, connected: %s, meta: unpacked: %s",
                    hexId,
                    unknownDevice.connected,
                    byteArrayToString(rawMeta),
                    metaData);
            unknownDevice.lastSeenTimestamp = System.currentTimeMillis();
            mDeviceMap.put(address, unknownDevice);
            mPeerBroadcaster.propagatePeerList(mDeviceMap);
        }
    }
}
