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
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.ParcelUuid;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;

class Scanner {

    interface ProximityCallback {
        void proximityChanged(Set<Peer> peers);
    }

    private final ProximityCallback mProximityCallback;

    private final static String TAG = "@aura/ble/scanner";

    private static Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private final Context mContext;
    private Handler mHandler;
    private UUID mServiceUuid;
    private UUID mSlogan1Uuid;
    private UUID mSlogan2Uuid;
    private UUID mSlogan3Uuid;
    private boolean mQueued = false;
    private boolean mInactive = false;

    private static final long PEER_FORGET_AFTER = 1000 * 60 * 2;
    private static final long PEER_REFRESH_AFTER = 1000 * 20;
    private static final long PEER_CONNECT_TIMEOUT = 1000 * 10;

    private HashMap<String, Device> devices = new HashMap<>();

    private Set<String> mSlogansAtLastPropagation = new HashSet<>();

    Scanner(UUID serviceUuid,
            UUID slogan1Uuid,
            UUID slogan2Uuid,
            UUID slogan3Uuid,
            Context context,
            ProximityCallback proximityCallback) {
        mServiceUuid = serviceUuid;
        mSlogan1Uuid = slogan1Uuid;
        mSlogan2Uuid = slogan2Uuid;
        mSlogan3Uuid = slogan3Uuid;
        mContext = context;
        mProximityCallback = proximityCallback;
    }

    void start() {
        mHandler = new Handler();
        scan();
        returnControl();
    }

    private void returnControl() {
        if (mQueued || mInactive) {
            return;
        }
        mHandler.postDelayed(this::actOnState, 300);
        mQueued = true;
    }

    void stop() {
        w(TAG, "onStop called, destroying");
        mInactive = true;
        for (String address : devices.keySet()) {

            Device device = devices.get(address);
            if (device.bt.gatt != null) {
                // TODO check if already closed?
                device.bt.gatt.close();
            }
            device.bt.device = null;
            device.bt.service = null;
        }
        mHandler.post(() -> {
            devices.clear();
        });
        mHandler = null;
    }

    /**
     * Updated contain all peers for which slogans have been retrieved and their slogans.
     * mProximityCallback is invoked only if new slogans were found or existing ones are gone.
     */
    private void propagateChanges() {

        v(TAG, "Checking if slogan changes need propagation");
        Set<String> slogans = new HashSet<>();
        for (Device device : devices.values()) {
            if (device.slogan1 != null && !device.slogan1.equals("")) {
                slogans.add(device.slogan1);
            }
            if (device.slogan2 != null && !device.slogan2.equals("")) {
                slogans.add(device.slogan2);
            }
            if (device.slogan3 != null && !device.slogan3.equals("")) {
                slogans.add(device.slogan3);
            }
        }

        // A kingdom for immutability and first class functions
        Set<String> gone = new HashSet<>();
        gone.addAll(mSlogansAtLastPropagation);
        gone.removeAll(slogans);

        // A kingdom for immutability and first class functions
        Set<String> found = new HashSet<>();
        found.addAll(slogans);
        found.removeAll(mSlogansAtLastPropagation);

        if (found.size() > 0 || gone.size() > 0) {
            d(TAG, "Slogans changed, devices: %d, slogans: %d found (%s) and %d gone (%s)", devices.size(), found.size(), found, gone.size(), gone);

            mProximityCallback.proximityChanged(buildPeers());
            mSlogansAtLastPropagation = slogans;
        } else {
            v(TAG, "No slogans changed, nothing to propagates, slogans: %d", slogans.size());
        }
    }

    Set<Peer> buildPeers() {
        Set<Peer> peers = new HashSet<>();
        for (Device device : devices.values()) {
            Peer peer = new Peer();
            peer.mLastSeenTimestamp = device.lastSeenTimestamp;
            peer.mSuccessfulRetrievals = device.stats.mSuccessfulRetrievals;

            if (device.slogan1 != null && !device.slogan1.equals("")) {
                peer.mSlogans.add(Slogan.create(device.slogan1));
            }
            if (device.slogan2 != null && !device.slogan2.equals("")) {
                peer.mSlogans.add(Slogan.create(device.slogan2));
            }
            if (device.slogan3 != null && !device.slogan3.equals("")) {
                peer.mSlogans.add(Slogan.create(device.slogan3));
            }
            peers.add(peer);
        }
        return peers;
    }

    @FunctionalInterface
    interface CurrentPeersCallback {
        void currentPeers(Set<Peer> peers);
    }

    /**
     * Avoids concurrent access to mPeers
     */
    void requestPeers(CurrentPeersCallback currentPeersCallback) {
        mHandler.post(() -> {
            currentPeersCallback.currentPeers(buildPeers());
        });
    }

    private void actOnState() {
        mQueued = false;

        long now = System.currentTimeMillis();

        for (String address : devices.keySet()) {

            Device device = devices.get(address);

            try {
                if (device.shouldDisconnect) {
                    i(TAG, "Disconnecting device, device: %s", address);
                    device.bt.gatt.close();
                    device.bt.gatt = null;
                    device.bt.service = null;
                    device.connected = false;
                    device.shouldDisconnect = false;
                    device.lastConnectAttempt = null;
                    device.isDiscoveringServices = false;
                    device.isFetchingSlogan = false;

                    // Giving the BT some air before we do the next connection attempt
                    continue;
                }

                if (!device.connected) {
                    if (device.lastConnectAttempt != null && now - device.lastConnectAttempt <= PEER_CONNECT_TIMEOUT) {
//                        v(TAG, "Nothing to do, connection attempt is in progress, device: %s", address);

                    } else if (device.lastConnectAttempt != null && now - device.lastConnectAttempt > PEER_CONNECT_TIMEOUT) {
                        d(TAG, "Connection timeout, closing gatt, device: %s", address);
                        device.shouldDisconnect = true;
                        device.stats.mErrors++;

                    } else if (now - device.lastSeenTimestamp > PEER_FORGET_AFTER) {
                        v(TAG, "Forgetting device, device: %s", address);
                        device.bt.device = null;
                        mHandler.post(() -> {
                            devices.remove(address);
                            propagateChanges();
                        });

                    } else if (device.lastFullRetrievalTimestamp == null || now - device.lastFullRetrievalTimestamp > PEER_REFRESH_AFTER) {
                        if (device.lastFullRetrievalTimestamp == null) {
                            d(TAG, "Connecting to gatt server (no prior successful retrieval), device: %s", address);
                        } else {
                            d(TAG, "Connecting to gatt server to refresh, device: %s", address);
                        }
                        device.connectionAttempts++;
                        device.lastConnectAttempt = now;
                        device.slogan1fresh = false;
                        device.slogan2fresh = false;
                        device.slogan3fresh = false;
                        device.bt.gatt = device.bt.device.connectGatt(mContext, false, mGattCallback);

                    } else {
//                        v(TAG, "Nothing to do for disconnected device, device: %s", address);
                    }
                    continue;
                }

                // device is currently connected

                if (device.bt.service == null && !device.isDiscoveringServices) {
                    device.isDiscoveringServices = true;
                    i(TAG, "Connected to %s, discovering services", address);
                    device.bt.gatt.discoverServices();
                    continue;
                }
                if (device.bt.service == null) {
                    v(TAG, "Still discovering services, device: %s", address);
                    continue;
                }

                // device has a service and is not discovering

                if (device.isFetchingSlogan) {
                    v(TAG, "Still fetching slogan, device: %s", address);
                    continue;
                }

                if (!device.slogan1fresh) {
                    requestSlogan(device, mSlogan1Uuid);
                    continue;
                }
                if (!device.slogan2fresh) {
                    requestSlogan(device, mSlogan2Uuid);
                    continue;
                }
                if (!device.slogan3fresh) {
                    requestSlogan(device, mSlogan3Uuid);
                    continue;
                }

                d(TAG, "All slogans fresh, should disconnect, address: %s", address);
                d(TAG, "slogan1: %s, device: %s", device.slogan1, address);
                d(TAG, "slogan2: %s, device: %s", device.slogan2, address);
                d(TAG, "slogan3: %s, device: %s", device.slogan3, address);
                device.lastFullRetrievalTimestamp = now;
                device.stats.mSuccessfulRetrievals++;
                device.shouldDisconnect = true;
            } catch (Exception e) {
                if (e instanceof DeadObjectException) {
                    w(TAG, "Thread seems to have been discarded of, caught a DeadObjectException");
                    return;
                }
                e(TAG, "Unhandled exception, device: %s", device.toLogString());
                throw e;
            }
        }
        returnControl();
    }

    private void requestSlogan(Device device, UUID uuid) {
        device.isFetchingSlogan = true;
        d(TAG, "Requesting characteristic, gatt: %s, characteristic: %s", device.bt.device.getAddress(), uuid);
        BluetoothGattCharacteristic chara = device.bt.service.getCharacteristic(uuid);
        if (!device.bt.gatt.readCharacteristic(chara)) {
            d(TAG, "Failed to request slogan. Disconnecting, gatt: %s, characteristic: %s", device.bt.device.getAddress(), uuid);
            device.shouldDisconnect = true;
            device.stats.mErrors++;
        }
    }

    private void scanningUnsupported() {
        d(TAG, "Scanning seems to be unsupported on this device");
        stop();
        // TODO implement
    }

    private void scan() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            scanningUnsupported();
            return;
        }
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        if (scanner == null) {
            scanningUnsupported();
            return;
        }

        ScanSettings settings = new ScanSettings.Builder()
                //   .setCallbackType(CALLBACK_TYPE_ALL_MATCHES)
                //   .setMatchMode(MATCH_NUM_MAX_ADVERTISEMENT)
                .setScanMode(SCAN_MODE_BALANCED)
                .build();

        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(mServiceUuid))
                .build());

        scanner.startScan(scanFilters, settings, new ScanCallback() {

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                v(TAG, "onScanResult callbackType: %d, result: %s", callbackType, result.getDevice().getAddress());

                handleResults(new ScanResult[]{result});
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                v(TAG, "onBatchScanResults callbackType: %d, result: %s", results == null ? "null" : results.toString());
                if (results != null) {
                    handleResults((ScanResult[]) results.toArray());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                d(TAG, "onScanFailed errorCode: %s", BtConst.nameScanError(errorCode));
            }
        });
        i(TAG, "started scanning");
    }

    private boolean assertPeer(String address, BluetoothGatt gatt, String operation) {
        if (devices.containsKey(address)) {
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
            String address = gatt.getDevice().getAddress();
            d(TAG, "onConnectionStateChange, gatt: %s, status: %s, newState: %s", address, BtConst.nameStatus(status), BtConst.nameConnectionState(newState));
            if (!assertPeer(address, gatt, "onConnectionStateChange")) {
                return;
            }

            if (newState == STATE_CONNECTED) {
                devices.get(address).connected = true;
                returnControl();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                d(TAG, "Disconnected from %s", address);
                devices.get(address).connected = false;
                returnControl();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String address = gatt.getDevice().getAddress();
            v(TAG, "onServicesDiscovered, gatt: %s, status: %s", address, BtConst.nameStatus(status));
            if (!assertPeer(address, gatt, "onServicesDiscovered")) {
                return;
            }
            Device device = devices.get(address);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                w(TAG, "onServicesDiscovered unsuccessful, status: %s", BtConst.nameStatus(status));
                device.shouldDisconnect = true;
                device.stats.mErrors++;
                returnControl();
                return;
            }


            d(TAG, "Discovered %d services, gatt: %s, services: %s", gatt.getServices().size(), address, gatt.getServices().toString());

            BluetoothGattService service = gatt.getService(mServiceUuid);

            if (service == null) {
                d(TAG, "Service is null, disconnecting, address: %s", address);
                device.shouldDisconnect = true;
                device.stats.mErrors++;
                returnControl();
                return;
            }
            device.isDiscoveringServices = false;
            device.bt.service = service;
            returnControl();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String address = gatt.getDevice().getAddress();
            d(TAG, "onCharacteristicRead, gatt: %s, characteristic: %s, status: %s", address, characteristic.getUuid(), BtConst.nameStatus(status));

            if (!assertPeer(address, gatt, "onCharacteristicRead")) {
                return;
            }

            Device device = devices.get(address);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                w(TAG, "onCharacteristicRead unsuccessful");
                device.shouldDisconnect = true;
                device.stats.mErrors++;
                returnControl();
                return;
            }
            byte[] value = characteristic.getValue();
            if (value == null) {
                w(TAG, "Retrieved null slogan, address: %s", address);
                device.shouldDisconnect = true;
                device.stats.mErrors++;
                returnControl();
                return;
            }

            UUID uuid = characteristic.getUuid();
            String slogan = new String(value, UTF8_CHARSET);
            d(TAG, "Retrieved slogan, device: %s, uuid: %s, slogan: %s", address, uuid, slogan);
            boolean changed = false;
            if (mSlogan1Uuid.equals(uuid)) {
                device.slogan1 = slogan;
                device.slogan1fresh = true;
                changed = true;

            } else if (mSlogan2Uuid.equals(uuid)) {
                device.slogan2 = slogan;
                device.slogan2fresh = true;
                changed = true;

            } else if (mSlogan3Uuid.equals(uuid)) {
                device.slogan3 = slogan;
                device.slogan3fresh = true;
                changed = true;
            } else {
                w(TAG, "Characteristic retrieved matches no slogan UUID, address: %s, uuid: %s", address, uuid);
            }
            device.isFetchingSlogan = false;
            if (changed) {
                propagateChanges();
            }
            returnControl();
        }
    };

    private void handleResults(ScanResult[] results) {
        for (ScanResult result : results) {
            String address = result.getDevice().getAddress();
            if (devices.containsKey(address)) {
//                v(TAG, "Nothing to do, device already known, device: %s", address);
                devices.get(address).lastSeenTimestamp = System.currentTimeMillis();
            } else {
                i(TAG, "Device %s is yet unknown", address);
                mHandler.post(() -> {
                    Device device = Device.create(result.getDevice());
                    device.lastSeenTimestamp = System.currentTimeMillis();
                    devices.put(address, device);
                });
            }
        }
    }
}
