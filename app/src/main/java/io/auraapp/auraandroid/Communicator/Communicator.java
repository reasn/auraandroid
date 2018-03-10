package io.auraapp.auraandroid.Communicator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.PermissionMissingActivity;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.main.MainActivity;

import static io.auraapp.auraandroid.common.EmojiHelper.replaceShortCode;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.w;
import static java.lang.String.format;

/**
 * Runs in a separate process
 * Thx to https://medium.com/@rotxed/going-multiprocess-on-android-52975ed8863c
 */
public class Communicator extends Service {

    private final static String TAG = "@aura/ble/communicator";

    private static final int FOREGROUND_NOTIFICATION_ID = 1338;
    /**
     * The time to remember BT_TURNING_ON events for.
     * Assumption: If Aura is not working properly because there's a problem with the BT stack,
     * users might repeatedly turn BT on and off. If that is the case an explanatory dialog will be shown.
     * This number sets the interval that's being considered "recent" for the number of clicks.
     * I.e. If set to 2 minutes, and within that timeframe the user clicks more
     * than RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD times, an alert is shown in MainActivity
     *
     * @see MainActivity#showBrokenBtStackAlert
     */
    private static final int RECENT_BT_TURNING_ON_EVENTS_RECENT_TIMEFRAME = 1000 * 60;
    public static final int RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD = 2;
    private Advertiser mAdvertiser;
    private Scanner mScanner;
    private boolean mRunning = false;
    private Handler mHandler;
    private final AdvertisementSet mAdvertisementSet = new AdvertisementSet();
    private boolean mIsRunningInForeground = false;
    private Set<Long> btTurningOnTimestamps = new TreeSet<>();

    private int mPeerSloganCount = 0;

    private final CommunicatorState mState = new CommunicatorState();
    private Set<Peer> mLastKnownPeers = null;

    @FunctionalInterface
    interface OnErrorCallback {
        void onUnrecoverableError();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        d(TAG, "onStartCommand, intent: %s, flags: %d, startId: %d", intent, flags, startId);

        if (!mRunning) {
            mRunning = true;
            init();
        }
        if (intent == null) {
            // Both intents to start the service and ones with actual payload end up in this method.
            // Intents to only start the service are null and can be ignored.
            actOnState(false);
        } else {
            // handleIntent implicitly calls actOnState()
            mHandler.post(() -> handleIntent(intent));
        }
        return START_STICKY;
    }

    private void init() {
        // Can't do this in constructor because R.string resources are not yet available
        d(TAG, "Initializing communicator");
        mHandler = new Handler();

        mAdvertiser = new Advertiser(
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE),
                mAdvertisementSet,
                this,
                (byte version, int id) -> {
                    mState.mVersion = version;
                    mState.mId = id;
                    sendState();
                },
                () -> {
                    mState.mBluetoothRestartRequired = true;
                    updateBtState();
                    actOnState(true);
                }
        );

        PeerBroadcaster broadcaster = new PeerBroadcaster(
                (Set<Peer> peers) -> mHandler.post(() -> {
                    if (!(peers instanceof Serializable)) {
                        throw new RuntimeException("peers must be serializable");
                    }
                    {
                        // Update notification
                        int peerSloganCount = 0;
                        for (Peer peer : peers) {
                            peerSloganCount += peer.mSlogans.size();
                        }
                        if (peerSloganCount != mPeerSloganCount) {
                            mPeerSloganCount = peerSloganCount;
                            updateForegroundNotification();
                        }
                    }

                    sendBroadcast(IntentFactory.peerListUpdated(peers, mState));
                    d(TAG, "Sent peer list intent with %d peers", peers.size());
                }),
                (Peer peer) -> {
                    sendBroadcast(IntentFactory.peerUpdated(peer));
                    d(TAG, "Sent peer update intent, id: %s, slogans: %d", peer.mId, peer.mSlogans.size());
                });

        mScanner = new Scanner(
                this,
                broadcaster,
                () -> {
                    mState.mBluetoothRestartRequired = true;
                    updateBtState();
                    actOnState(true);
                });

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context $, Intent intent) {
                d(TAG, "onReceive, intent: %s", intent);
                mHandler.post(() -> handleIntent(intent));
            }
        }, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        d(TAG, "Started listening to %s events", BluetoothAdapter.ACTION_STATE_CHANGED);

        updateBtState();
        actOnStateWhileWaitingForPermissions();
    }

    private void actOnStateWhileWaitingForPermissions() {
        mState.mHasPermission = PermissionHelper.granted(this);

        actOnState(false, () -> {
            if (!mState.mHasPermission) {
                mHandler.postDelayed(this::actOnStateWhileWaitingForPermissions, 500);
            }
        });
    }

    /**
     * The order of conditions should be synchronized with that in MainActivity::updateCommunicatorState
     * <p>
     * Thanks to https://gist.github.com/kristopherjohnson/6211176
     */
    private void updateForegroundNotification() {

        Class activity = mState.mHasPermission
                ? MainActivity.class
                : PermissionMissingActivity.class;

        String title;
        String text = "";
        if (!mState.mHasPermission) {
            title = getString(R.string.ui_notification_no_permission_title);
            text = getString(R.string.ui_notification_no_permission_text);

        } else if (!mState.mBtEnabled) {
            title = getString(R.string.ui_notification_bt_disabled_title);
            text = getString(R.string.ui_notification_bt_disabled_text);

        } else if (!mState.mBleSupported) {
            title = getString(R.string.ui_notification_ble_not_supported_title);
            text = getString(R.string.ui_notification_ble_not_supported_text);

        } else if (!mState.mShouldCommunicate) {
            title = getString(R.string.ui_notification_disabled_title);

        } else {
            if (!mState.mAdvertisingSupported) {
                title = getString(R.string.ui_notification_advertising_not_supported_title);
                text = getString(R.string.ui_notification_advertising_not_supported_text);
            } else if (!mState.mAdvertising) {
                w(TAG, "Not advertising although it is possible.");
                title = getString(R.string.ui_notification_on_not_active_title);
            } else if (!mState.mScanning) {
                w(TAG, "Not scanning although it is possible.");
                title = getString(R.string.ui_notification_on_not_active_title);
            } else {
                title = getString(R.string.ui_notification_on_title);
            }
        }

        if (mPeerSloganCount > 0) {
            title += format(Locale.ENGLISH, ". %d :thought_balloon:", mPeerSloganCount);
        }

        title = replaceShortCode(title);
        text = replaceShortCode(text);

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                IntentFactory.showActivity(getApplicationContext(), activity),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, createNotificationChannel())
                : new Notification.Builder(this);
        builder.setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_notification)
                .setTicker(title)
                .setContentIntent(contentIntent);

        if (text.length() > 0) {
            builder.setContentText(text);
        }
        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
    }


    /**
     * Thanks to https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("communicator_channel", "Aura", NotificationManager.IMPORTANCE_HIGH);
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        channel.setShowBadge(false);
        NotificationManager notificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (notificationManager == null) {
            throw new RuntimeException("Could not fetch NOTIFICATION_SERVICE");
        }
        notificationManager.createNotificationChannel(channel);
        return channel.getId();
    }

    private void actOnState(boolean forceSend) {
        actOnState(forceSend, null);
    }

    private void actOnState(boolean forceSend, @Nullable Runnable after) {

        mHandler.post(() -> {
            boolean stateChanged = false;

            boolean shouldAdvertise = mState.mBtEnabled
                    && mState.mShouldCommunicate
                    && mState.mBleSupported
                    && mState.mAdvertisingSupported
                    && mState.mHasPermission;

            boolean shouldScan = mState.mBtEnabled
                    && mState.mShouldCommunicate
                    && mState.mHasPermission;

            if (shouldAdvertise) {
                if (!mState.mAdvertising) {
                    mAdvertiser.start();
                    stateChanged = true;
                    mState.mAdvertising = true;
                }

            } else if (mState.mAdvertising) {
                mAdvertiser.stop();
                mState.mAdvertising = false;
                stateChanged = true;
            }

            if (shouldScan) {
                if (!mState.mScanning) {
                    mScanner.start();
                    mState.mScanning = true;
                    stateChanged = true;
                }

            } else if (mState.mScanning) {
                mScanner.stop();
                mState.mScanning = false;
                stateChanged = true;
            }

            if (mState.mShouldCommunicate && !mIsRunningInForeground) {
                updateForegroundNotification();
                mIsRunningInForeground = true;

            } else if (!mState.mShouldCommunicate && mIsRunningInForeground) {
                stopForeground(true);
                mIsRunningInForeground = false;
            }

            if (stateChanged || forceSend) {
                sendState();
            }
            if (stateChanged) {
                if (mIsRunningInForeground) {
                    updateForegroundNotification();
                }
            }
            if (after != null) {
                after.run();
            }
        });
    }

    /**
     * Will affect mBtEnabled, mBleSupported
     * The HardwareIds rule is suppressed because the id is hashed to a 1024-elements space and only used for logging
     */
    private void updateBtState() {
        d(TAG, "Updating BT state");

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (!mState.mBtEnabled) {
                i(TAG, "Bluetooth adapter available");
            }
            mState.mBtEnabled = true;
        } else {
            mState.mBtEnabled = false;
            // Disabling BT resets the necessity to restart BT
            mState.mBluetoothRestartRequired = false;
            d(TAG, "Bluetooth is currently unavailable");
        }

        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            mState.mBleSupported = true;
        } else {
            d(TAG, "BLE scanning is currently unavailable");
            mState.mBleSupported = false;
        }
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeAdvertiser() != null) {
            mState.mAdvertisingSupported = true;
        } else {
            d(TAG, "BLE advertising is currently unavailable");
            mState.mAdvertisingSupported = false;
        }
    }

    private void sendState() {
        d(TAG, "Sending state, state: %s", mState);
        sendBroadcast(IntentFactory.communicatorState(mState));
    }

    private void handleIntent(Intent intent) {

        if (intent == null) {
            w(TAG, "Received null intent");
            return;
        }

        final String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            w(TAG, "Bluetooth state changed, state: %s", BtConst.nameAdapterState(state));
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    mState.mBtEnabled = false;
                    mState.mBtTurningOn = false;
                    // Disabling BT resets the necessity to restart BT
                    mState.mBluetoothRestartRequired = false;
                    break;

                case BluetoothAdapter.STATE_ON:
                    mState.mBtTurningOn = false;
                    updateBtState();
                    break;

                case BluetoothAdapter.STATE_TURNING_ON:
                    mState.mBtEnabled = false;
                    mState.mBtTurningOn = true;
                    // urgh. mutability again. Streams only supported with API level 24+
                    Set<Long> filtered = new TreeSet<>();
                    for (Long timestamp : btTurningOnTimestamps) {
                        if (timestamp > RECENT_BT_TURNING_ON_EVENTS_RECENT_TIMEFRAME) {
                            filtered.add(timestamp);
                        }
                    }
                    filtered.add(System.currentTimeMillis());
                    btTurningOnTimestamps = filtered;
                    mState.mRecentBtTurnOnEvents = btTurningOnTimestamps.size();
                    break;
                default:
                    w(TAG, "Unknown BT state received, state: %d", state);
                    break;
            }
            actOnState(true);

        } else if (IntentFactory.INTENT_ENABLE_ACTION.equals(action)) {
            mState.mShouldCommunicate = true;
            actOnState(true);

        } else if (IntentFactory.INTENT_DISABLE_ACTION.equals(action)) {
            mScanner.requestPeers((Set<Peer> peers) -> {
                for (Peer peer : peers) {
                    peer.mSynchronizing = false;
                }
                mLastKnownPeers = peers;
                sendBroadcast(IntentFactory.peerListUpdated(peers, mState));
                d(TAG, "Updated peers with mSynchronizing=false and sent intent with %d peers", peers.size());
                mState.mShouldCommunicate = false;
                actOnState(true);
            });

        } else if (IntentFactory.INTENT_REQUEST_PEERS_ACTION.equals(action)) {

            mScanner.requestPeers((Set<Peer> peers) -> {
                sendBroadcast(IntentFactory.peerListUpdated(peers, mState));
                d(TAG, "Sent intent with %d peers", peers.size());
            });

        } else if (IntentFactory.INTENT_MY_SLOGANS_CHANGED_ACTION.equals(action)) {

            Bundle extras = intent.getExtras();

            if (extras == null) {
                w(TAG, "No extras on intent");
                return;
            }

            @SuppressWarnings("unchecked")
            String[] mySlogans = extras.getStringArray(IntentFactory.INTENT_MY_SLOGANS_CHANGED_EXTRA_SLOGANS);
            if (mySlogans == null) {
                w(TAG, "No slogans retrieved from intent");
                return;
            }
            boolean currentlyAdvertisingOnDifferentSlogans = mAdvertisementSet.mSlogansSet;
            mAdvertisementSet.setSlogans(mySlogans);
            if (currentlyAdvertisingOnDifferentSlogans && mState.mAdvertising) {
                mAdvertiser.increaseVersion();
            }
            sendState();
        } else {
            w(TAG, "Received unknown intent, intent: %s", intent);
        }
        // No statements must happen outside conditional because some things (e.g. requestPeers) happen asynchronously
    }

    @Override
    public void onDestroy() {
        w(TAG, "onDestroy called, destroying");

        mScanner.stop();
        mAdvertiser.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
