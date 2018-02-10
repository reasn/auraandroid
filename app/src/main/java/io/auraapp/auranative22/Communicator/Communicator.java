package io.auraapp.auranative22.Communicator;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Set;
import java.util.UUID;

import io.auraapp.auranative22.MainActivity;
import io.auraapp.auranative22.R;

import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.w;

public class Communicator extends Service {

    public static final String INTENT_LOCAL_SLOGANS_CHANGED_ACTION = "io.aurapp.aura1.v1localSlogansChanged";
    public static final String INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_1 = "io.auraapp.aura1.slogan1";
    public static final String INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_2 = "io.auraapp.aura1.slogan2";
    public static final String INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_3 = "io.auraapp.aura1.slogan3";

    public static final String INTENT_PEERS_CHANGED_ACTION = "io.auraapp.aura1.v1peerSlogansChanged";
    public static final String INTENT_PEERS_CHANGED_SLOGANS_FOUND = "io.auraapp.aura1.slogansFound";
    public static final String INTENT_PEERS_CHANGED_SLOGANS_GONE = "io.auraapp.aura1.slogansGone";

    private final static String TAG = "@aura/ble/communicator";

    public static final int FOREGROUND_ID = 1338;
    private Advertiser mAdvertiser;
    private Scanner mScanner;
    private boolean mRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!INTENT_LOCAL_SLOGANS_CHANGED_ACTION.equals(intent.getAction())) {
            w(TAG, "Received unknown intent, intent: %s", intent);
            return START_STICKY;
        }

        if (!mRunning) {
            mRunning = true;
            start();
        }
        handleIntent(intent);

        return START_STICKY;
    }

    private void handleIntent(Intent intent) {

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        if (intent.hasExtra(INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_1)) {
            mAdvertiser.setSlogan1(extras.getString(INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_1));
        }
        if (intent.hasExtra(INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_2)) {
            mAdvertiser.setSlogan2(extras.getString(INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_2));
        }
        if (intent.hasExtra(INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_3)) {
            mAdvertiser.setSlogan3(extras.getString(INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_3));
        }
    }

    private void start() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        final Notification notification = new Notification.Builder(this)
                .setContentTitle("ContentTitle")
                .setContentText("ContentTExt")
                .setContentIntent(pendingIntent)
                .setTicker("🍭ticker")
                .build();

        startForeground(FOREGROUND_ID, notification);

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
        final Context context = this;
        mScanner = new Scanner(
                serviceUuid,
                slogan1Uuid,
                slogan2Uuid,
                slogan3Uuid,
                this,
                (Set<String> found, Set<String> gone) -> {

                    Intent intent = new Intent(INTENT_PEERS_CHANGED_ACTION);
                    // The argument to toArray is required, otherwise Object[] is serialized resulting in broken payloads
                    intent.putExtra(INTENT_PEERS_CHANGED_SLOGANS_FOUND, found.toArray(new String[found.size()]));
                    intent.putExtra(INTENT_PEERS_CHANGED_SLOGANS_GONE, gone.toArray(new String[found.size()]));

                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                    d(TAG, "Sent intent %s", intent.getAction());
                }
        );

        new Handler().postDelayed(() -> {
            mScanner.start();
            mAdvertiser.start();
        }, 100);
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
