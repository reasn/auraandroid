package io.auraapp.auranative22.Communicator;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import io.auraapp.auranative22.MainActivity;
import io.auraapp.auranative22.R;

import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.w;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!mRunning) {
            mRunning = true;
            makeForegroundService();
            startCommunicator();
        }
        handleIntent(intent);
        return START_STICKY;
    }

    /**
     * Thanks to https://gist.github.com/kristopherjohnson/6211176
     */
    private void makeForegroundService() {

        Intent showActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
        showActivityIntent.setAction(Intent.ACTION_MAIN);
        showActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        showActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                showActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle("🔮 Your Aura is on")
//                .setContentText("You're Aura is visible")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(contentIntent)
                .build();
        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
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

    private void startCommunicator() {

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

        mScanner.start();
        mAdvertiser.start();
        d(TAG, "Communicator started");
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
