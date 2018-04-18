package io.auraapp.auraandroid.Communicator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
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
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfile;

import static io.auraapp.auraandroid.common.Config.PEERS_CHANGED_NOTIFICATION_LIGHT_PATTERN;
import static io.auraapp.auraandroid.common.EmojiHelper.replaceShortCode;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;
import static java.lang.String.format;

/**
 * Runs in a separate process
 * Thx to https://medium.com/@rotxed/going-multiprocess-on-android-52975ed8863c
 */
public class Communicator extends Service {

    static final byte PROTOCOL_VERSION = 1;

    private final static String TAG = "@aura/ble/communicator";

    private Advertiser mAdvertiser;
    private Scanner mScanner;
    private boolean mRunning = false;
    private Handler mHandler;
    private final AdvertisementSet mAdvertisementSet = new AdvertisementSet();
    private boolean mIsRunningInForeground = false;
    private Set<Long> btTurningOnTimestamps = new TreeSet<>();
    private int mPeerSloganCount = 0;
    private int notificationIndex = 0;
    private long lastNotification = 0;
    private final CommunicatorState mState = new CommunicatorState();

    @FunctionalInterface
    interface OnUnrecoverableBtErrorCallback {
        void onUnrecoverableBtError(String errorName);
    }

    @FunctionalInterface
    interface OnCorruptedStateCallback {
        void onCorruptedState(Exception exception);
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
                mAdvertisementSet,
                this,
                (byte version, int id) -> {
                    mState.mVersion = version;
                    mState.mId = id;
                    sendState();
                },
                errorName -> {
                    mState.mBluetoothRestartRequired = true;
                    mState.mLastError = errorName;
                    updateBtState();
                    actOnState(true);
                },
                exception -> {
                    // Thanks https://stackoverflow.com/questions/5883635/how-to-remove-all-callbacks-from-a-handler
                    mHandler.removeCallbacks(null);

                    e(TAG, "Corrupted state, stopping service, error: %s", exception == null ? "null" : exception.getMessage());
                    stopForeground(true);
                    mIsRunningInForeground = false;
                    stopSelf();
                }
        );

        PeerBroadcaster broadcaster = new PeerBroadcaster(
                peers -> mHandler.post(() -> {
                    if (!(peers instanceof Serializable)) {
                        throw new RuntimeException("peers must be serializable");
                    }
                    int peerSloganCount = 0;
                    for (Peer peer : peers) {
                        peerSloganCount += peer.mSlogans.size();
                    }
                    mPeerSloganCount = peerSloganCount;
                    sendBroadcast(IntentFactory.peerListUpdated(peers, mState));
                    d(TAG, "Sent peer list intent with %d peers", peers.size());
                }),
                (peer, contentChanged, sloganCount) -> {

                    mPeerSloganCount = sloganCount;

                    if (contentChanged) {
                        showPeerNotification();
                        // TODO notification prefs
                        d(TAG, "Content added, sending peer update intent, id: %s, slogans: %d",
                                Integer.toHexString(peer.mId),
                                peer.mSlogans.size());
                    } else {
                        v(TAG, "Sending peer update intent, id: %s, slogans: %d",
                                Integer.toHexString(peer.mId),
                                peer.mSlogans.size());
                    }
                    sendBroadcast(IntentFactory.peerUpdated(peer));
                });

        mScanner = new Scanner(
                this,
                broadcaster,
                errorName -> {
                    mState.mBluetoothRestartRequired = true;
                    mState.mLastError = errorName;
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

    private void showPeerNotification() {
        long now = System.currentTimeMillis();
        if (now - lastNotification > 5000) {
            updateForegroundNotification(true);
            lastNotification = now;
        }
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
    private void updateForegroundNotification(boolean newPeersFound) {
        v(TAG, "updating foreground notification, newPeersFound: %s", newPeersFound ? "yes" : "no");

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
                if (mPeerSloganCount > 0) {
                    // TODO pluralize
                    title += format(Locale.ENGLISH, ". %d :thought_balloon:", mPeerSloganCount);
                }
            }
        }


        title = replaceShortCode(title);
        text = replaceShortCode(text);

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                IntentFactory.showActivity(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);


        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(Communicator.this, createNotificationChannel(newPeersFound));
        } else {
            builder = new Notification.Builder(Communicator.this);
            if (newPeersFound) {
                builder.setLights(getResources().getColor(R.color.purple), PEERS_CHANGED_NOTIFICATION_LIGHT_PATTERN[0], PEERS_CHANGED_NOTIFICATION_LIGHT_PATTERN[1]);
                builder.setVibrate(Config.PEERS_CHANGED_NOTIFICATION_VIBRATION_PATTERN);
                builder.setNumber(++notificationIndex);
            }
        }

        builder.setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_notification)
                .setTicker(title)
                .setContentIntent(contentIntent);

        if (text.length() > 0) {
            builder.setContentText(text);
        }
        startForeground(Config.COMMUNICATOR_FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    /**
     * Thanks to https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel(boolean newPeersFound) {
        int importance = newPeersFound
                ? NotificationManager.IMPORTANCE_HIGH
                : NotificationManager.IMPORTANCE_NONE;
        NotificationChannel channel = new NotificationChannel("communicator_channel", "Aura", importance);
        channel.setImportance(importance);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        channel.setLightColor(R.color.purple);
        channel.setVibrationPattern(Config.PEERS_CHANGED_NOTIFICATION_VIBRATION_PATTERN);
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
                    && !mState.mBluetoothRestartRequired
                    && mState.mShouldCommunicate
                    && mState.mBleSupported
                    && mState.mAdvertisingSupported
                    && mState.mHasPermission;

            boolean shouldScan = mState.mBtEnabled
                    && !mState.mBluetoothRestartRequired
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
                    mState.mScanStartTimestamp = System.currentTimeMillis();
                    stateChanged = true;
                }

            } else if (mState.mScanning) {
                mScanner.stop();
                mState.mScanning = false;
                stateChanged = true;
            }

            if (mState.mShouldCommunicate && !mIsRunningInForeground) {
                updateForegroundNotification(false);
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
                    updateForegroundNotification(false);
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
            mState.mLastError = null;
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

        d(TAG, "handleIntent, action: %s", action);

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
                    mState.mLastError = null;
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
                        if (timestamp > Config.COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_RECENT_TIMEFRAME) {
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
            mState.mShouldCommunicate = false;
            actOnState(true);

        } else if (IntentFactory.INTENT_REQUEST_PEERS_ACTION.equals(action)) {

            mScanner.requestPeers((Set<Peer> peers) -> {
                sendBroadcast(IntentFactory.peerListUpdated(peers, mState));
                d(TAG, "Sent intent with %d peers", peers.size());
            });

        } else if (IntentFactory.INTENT_MY_PROFILE_CHANGED_ACTION.equals(action)) {

            if (!mState.mShouldCommunicate) {
                w(TAG, "Received %s intent while being not supposed to communicate. Starting to communicate to fulfill intent.", action);
                mState.mShouldCommunicate = true;
            }

            Bundle extras = intent.getExtras();

            if (extras == null) {
                w(TAG, "No extras on intent");
                return;
            }

            @SuppressWarnings("unchecked")
            MyProfile myProfile = (MyProfile) extras.getSerializable(IntentFactory.INTENT_MY_PROFILE_CHANGED_EXTRA_PROFILE);
            if (myProfile == null) {
                w(TAG, "No profile found in intent");
                return;
            }
            d(TAG, "Received profile %s, advertising: %s", myProfile.toString(), mState.mAdvertising);

            boolean advertisementChanged = false;

            String[] preparedSlogans = AdvertisementSet.prepareSlogans(myProfile.getSlogans());
            if (!Arrays.equals(mAdvertisementSet.mSlogans, preparedSlogans)) {
                mAdvertisementSet.mSlogans = preparedSlogans;
                mAdvertisementSet.increaseVersion();
                advertisementChanged = true;
            }

            String preparedProfile = AdvertisementSet.prepareProfile(myProfile.getColor(), myProfile.getName(), myProfile.getText());
            if (!preparedProfile.equals(mAdvertisementSet.getProfile())) {
                mAdvertisementSet.setProfile(preparedProfile);
                mAdvertisementSet.increaseVersion();
                advertisementChanged = true;
            }

            if (mState.mAdvertising && advertisementChanged) {
                i(TAG, "Updating advertisement, new version: %d, profile: %d chars, slogans: %d",
                        mAdvertisementSet.getVersion(),
                        mAdvertisementSet.getProfile().length(),
                        mAdvertisementSet.mSlogans.length);
                mAdvertiser.updateAdvertisement();
            }

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
