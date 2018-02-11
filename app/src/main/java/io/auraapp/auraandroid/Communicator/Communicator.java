package io.auraapp.auraandroid.Communicator;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import io.auraapp.auraandroid.PermissionMissingActivity;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.main.MainActivity;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.w;

/**
 * Runs in a separate process
 * Thx to https://medium.com/@rotxed/going-multiprocess-on-android-52975ed8863c
 */
public class Communicator extends Service {

    public static final String INTENT_MY_SLOGANS_CHANGED_ACTION = "io.aurapp.aura.mySlogansChanged";
    public static final String INTENT_MY_SLOGANS_CHANGED_SLOGANS = "io.auraapp.aura.mySlogans";

    public static final String INTENT_PEERS_CHANGED_ACTION = "io.auraapp.aura.peersUpdated";
    public static final String INTENT_PEERS_CHANGED_PEERS = "io.auraapp.aura.peers";

    private final static String TAG = "@aura/ble/communicator";

    public static final int FOREGROUND_NOTIFICATION_ID = 1338;
    private Advertiser mAdvertiser;
    private Scanner mScanner;
    private boolean mRunning = false;
    private Handler mHandler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!mRunning) {
            mRunning = true;

            mHandler = new Handler();
            if (!PermissionHelper.granted(this)) {
                makeForegroundService(false);
            }
            initialize();

            awaitPermissions();
        }
        handleIntent(intent);
        return START_STICKY;
    }

    private void awaitPermissions() {
        if (PermissionHelper.granted(this)) {
            makeForegroundService(true);
            mScanner.start();
            mAdvertiser.start();
            return;
        }
        mHandler.postDelayed(this::awaitPermissions, 500);
    }

    /**
     * Thanks to https://gist.github.com/kristopherjohnson/6211176
     */
    private void makeForegroundService(boolean permissionsGranted) {

        Class activity = permissionsGranted
                ? MainActivity.class
                : PermissionMissingActivity.class;

        String title = permissionsGranted
                ? "🔥 Your Aura is on"
                : "🔥 Your Aura is off";
        String text = permissionsGranted
                ? null
                : "Click to turn it on";

        Intent showActivityIntent = new Intent(getApplicationContext(), activity);
        showActivityIntent.setAction(Intent.ACTION_MAIN);
        showActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        showActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                showActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setContentTitle(title)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setTicker(title)
                .setContentIntent(contentIntent);

        if (text != null) {
            builder.setContentText(text);
        }
        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    private void handleIntent(Intent intent) {

        if (intent == null || !INTENT_MY_SLOGANS_CHANGED_ACTION.equals(intent.getAction())) {
            w(TAG, "Received unknown intent, intent: %s", intent);
            return;
        }

        Bundle extras = intent.getExtras();

        if (extras == null) {
            w(TAG, "No extras on intent");
            return;
        }

        @SuppressWarnings("unchecked")
        String[] mySlogans = extras.getStringArray(INTENT_MY_SLOGANS_CHANGED_SLOGANS);
        if (mySlogans == null) {
            w(TAG, "No slogans retrieved from intent");
            return;
        }
        mAdvertiser.setSlogan1(mySlogans.length > 0 ? mySlogans[0] : null);
        mAdvertiser.setSlogan2(mySlogans.length > 1 ? mySlogans[1] : null);
        mAdvertiser.setSlogan3(mySlogans.length > 2 ? mySlogans[2] : null);
    }

    private void initialize() {

        d(TAG, "Starting communicator");
        UUID serviceUuid = UUID.fromString(getString(R.string.ble_uuid_service));
        UUID slogan1Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_1));
        UUID slogan2Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_2));
        UUID slogan3Uuid = UUID.fromString(getString(R.string.ble_uuid_slogan_3));

        mAdvertiser = new Advertiser(
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE),
                serviceUuid,
                slogan1Uuid,
                slogan2Uuid,
                slogan3Uuid,
                this
        );
        mScanner = new Scanner(
                serviceUuid,
                slogan1Uuid,
                slogan2Uuid,
                slogan3Uuid,
                this,
                (Set<Peer> peers) -> {
                    if (!(peers instanceof Serializable)) {
                        throw new RuntimeException("peers must be serializable");
                    }
                    Intent intent = new Intent(INTENT_PEERS_CHANGED_ACTION);
                    intent.putExtra(INTENT_PEERS_CHANGED_PEERS, (Serializable) peers);
                    sendBroadcast(intent);

                    d(TAG, "Sent intent with %d peers, intent: %s", peers.size(), intent.getAction());
                }
        );
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
