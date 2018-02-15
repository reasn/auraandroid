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
import java.util.UUID;

import io.auraapp.auraandroid.PermissionMissingActivity;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.main.MainActivity;

import static io.auraapp.auraandroid.common.EmojiHelper.replaceAppEmoji;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.w;
import static java.lang.String.format;

/**
 * Runs in a separate process
 * Thx to https://medium.com/@rotxed/going-multiprocess-on-android-52975ed8863c
 */
public class Communicator extends Service {

    // incoming
    public static final String INTENT_ENABLE_ACTION = "io.aurapp.aura.enableCommunicator";
    public static final String INTENT_DISABLE_ACTION = "io.aurapp.aura.disableCommunicator";
    public static final String INTENT_REQUEST_PEERS_ACTION = "io.aurapp.aura.requestPeers";
    public static final String INTENT_MY_SLOGANS_CHANGED_ACTION = "io.aurapp.aura.mySlogansChanged";
    public static final String INTENT_MY_SLOGANS_CHANGED_SLOGANS_EXTRA = "io.auraapp.aura.mySlogansExtra";

    // outgoing
    public static final String INTENT_COMMUNICATOR_STATE_UPDATED_ACTION = "io.auraapp.aura.healthUpdated";

    public static final String INTENT_PEERS_UPDATE_ACTION = "io.auraapp.aura.peersUpdated";
    public static final String INTENT_PEERS_UPDATE_PEERS_EXTRA = "io.auraapp.aura.peersExtra";

    public static final java.lang.String INTENT_COMMUNICATOR_STATE_EXTRA = "io.aurapp.aura.stateExtra";

    private final static String TAG = "@aura/ble/communicator";

    public static final int FOREGROUND_NOTIFICATION_ID = 1338;
    private Advertiser mAdvertiser;
    private Scanner mScanner;
    private boolean mRunning = false;
    private Handler mHandler;
    private boolean mIsRunningInForeground = false;
    private int mPeerSloganCount = 0;

    CommunicatorState mState = new CommunicatorState();

    @FunctionalInterface
    interface OnBleSupportChangedCallback {
        void onBleSupportChanged();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        d(TAG, "onStartCommand, intent: %s, flags: %d, startId: %d", intent, flags, startId);

        if (!mRunning) {
            mRunning = true;
            init();
        }
        handleIntent(intent);
        return START_STICKY;
    }

    private void init() {
        // Can't do this in constructor because R.string resources are not yet available
        d(TAG, "Initializing communicator");
        UUID serviceUuid = UUID.fromString(getString(R.string.ble_uuid_service));
        UUID slogan1Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_1));
        UUID slogan2Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_2));
        UUID slogan3Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_3));

        mHandler = new Handler();

        mAdvertiser = new Advertiser(
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE),
                serviceUuid,
                slogan1Uuid,
                slogan2Uuid,
                slogan3Uuid,
                this,
                () -> {
                    updateBtState();
                    actOnState();
                }
        );
        mScanner = new Scanner(
                serviceUuid,
                slogan1Uuid,
                slogan2Uuid,
                slogan3Uuid,
                this,
                (Set<Peer> peers) -> mHandler.post(() -> {
                    if (!(peers instanceof Serializable)) {
                        throw new RuntimeException("peers must be serializable");
                    }
                    int peerSloganCount = 0;
                    for (Peer peer : peers) {
                        peerSloganCount += peer.mSlogans.size();
                    }
                    if (peerSloganCount != mPeerSloganCount) {
                        mPeerSloganCount = peerSloganCount;
                        updateForegroundNotification();
                    }

                    Intent intent = new Intent(INTENT_PEERS_UPDATE_ACTION);
                    intent.putExtra(INTENT_PEERS_UPDATE_PEERS_EXTRA, (Serializable) peers);
                    sendBroadcast(intent);

                    d(TAG, "Sent intent with %d peers, intent: %s", peers.size(), intent.getAction());
                })
        );

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context $, Intent intent) {
                d(TAG, "onReceive, intent: %s", intent);
                handleIntent(intent);
            }
        }, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        d(TAG, "Started listening to %s events", BluetoothAdapter.ACTION_STATE_CHANGED);

        updateBtState();
        actOnStateWhileWaitingForPermissions();
    }

    private void actOnStateWhileWaitingForPermissions() {
        mState.mHasPermission = PermissionHelper.granted(this);

        actOnState(() -> {
            if (!mState.mHasPermission) {
                mHandler.postDelayed(this::actOnStateWhileWaitingForPermissions, 500);
            }
        });
    }

    /**
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
            title += format(Locale.ENGLISH, " - %d :thought_balloon:", mPeerSloganCount);
        }

        title = replaceAppEmoji(title);
        text = replaceAppEmoji(text);

        Intent showActivityIntent = new Intent(getApplicationContext(), activity);
        showActivityIntent.setAction(Intent.ACTION_MAIN);
        showActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        showActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, showActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, createNotificationChannel())
                : new Notification.Builder(this);
        builder.setContentTitle(title)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
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


    void actOnState() {
        actOnState(null);
    }

    void actOnState(@Nullable Runnable after) {

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

            if (stateChanged) {
                sendState();
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
     */
    private void updateBtState() {
        d(TAG, "Updating BT state");

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            mState.mBtEnabled = true;
        } else {
            mState.mBtEnabled = false;
            d(TAG, "Bluetooth is currently unavailable");
        }

        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null) {
            mState.mBleSupported = true;
        } else {
            d(TAG, "BLE scanning is currently unavailable");
            mState.mBleSupported = false;
        }
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeAdvertiser() != null && !mAdvertiser.mUnrecoverableAdvertisingError) {
            mState.mAdvertisingSupported = true;
        } else {
            d(TAG, "BLE advertising is currently unavailable");
            mState.mAdvertisingSupported = false;
        }
    }

    private void sendState() {
        d(TAG, "Sending state");
        Intent stateIntent = new Intent(INTENT_COMMUNICATOR_STATE_UPDATED_ACTION);
        stateIntent.putExtra(INTENT_COMMUNICATOR_STATE_EXTRA, mState);
        sendBroadcast(stateIntent);
    }

    private void handleIntent(Intent intent) {

        if (intent == null) {
            w(TAG, "Received null intent");
            return;
        }

        final String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    this.mState.mBtEnabled = false;
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    this.mState.mBtEnabled = false;
                    break;
                case BluetoothAdapter.STATE_ON:
                    updateBtState();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    this.mState.mBtEnabled = false;
                    break;
                default:
                    w(TAG, "Unknown BT state received, state: %d", state);
                    break;
            }
            actOnState(this::sendState);

        } else if (INTENT_ENABLE_ACTION.equals(action)) {
            mState.mShouldCommunicate = true;
            actOnState(this::sendState);

        } else if (INTENT_DISABLE_ACTION.equals(action)) {
            mState.mShouldCommunicate = false;
            actOnState(this::sendState);

        } else if (INTENT_REQUEST_PEERS_ACTION.equals(action)) {

            mScanner.requestPeers((Set<Peer> peers) -> {
                Intent responseIntent = new Intent(INTENT_PEERS_UPDATE_ACTION);
                responseIntent.putExtra(INTENT_PEERS_UPDATE_PEERS_EXTRA, (Serializable) peers);
                responseIntent.putExtra(INTENT_COMMUNICATOR_STATE_EXTRA, mState);
                sendBroadcast(responseIntent);
                d(TAG, "Sent intent with %d peers, intent: %s", peers.size(), responseIntent.getAction());
            });

        } else if (INTENT_MY_SLOGANS_CHANGED_ACTION.equals(action)) {

            Bundle extras = intent.getExtras();

            if (extras == null) {
                w(TAG, "No extras on intent");
                return;
            }

            @SuppressWarnings("unchecked")
            String[] mySlogans = extras.getStringArray(INTENT_MY_SLOGANS_CHANGED_SLOGANS_EXTRA);
            if (mySlogans == null) {
                w(TAG, "No slogans retrieved from intent");
                return;
            }
            mAdvertiser.setSlogan1(mySlogans.length > 0 ? mySlogans[0] : null);
            mAdvertiser.setSlogan2(mySlogans.length > 1 ? mySlogans[1] : null);
            mAdvertiser.setSlogan3(mySlogans.length > 2 ? mySlogans[2] : null);
            sendState();
        } else {
            w(TAG, "Received unknown intent, intent: %s", intent);
        }
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
