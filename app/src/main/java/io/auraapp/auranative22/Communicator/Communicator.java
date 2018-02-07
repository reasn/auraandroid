package io.auraapp.auranative22.Communicator;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.UUID;

import io.auraapp.auranative22.MainActivity;
import io.auraapp.auranative22.R;

import static io.auraapp.auranative22.FormattedLog.w;

public class Communicator extends Service {

    private final static String TAG = "@aura/ble/scanner";

    public static final int FOREGROUND_ID = 1338;
    private Handler mHandler;
    private Advertiser mAdvertiser;
    private Scanner mScanner;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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
                "Helloo World, Hello!1Helloo World, 🚀 Hello!1Helloo World, Hello!1Helloo World, H",
                "Fappoo LorpdFappo!1FappoLorpdFappo!1FappoLorpdFappo!1FappoLorpdF nanunana wadatap",
                "The lazy chicken jumps over the running dog on its way to the alphabet. Cthullu !",
                this
        );
        mScanner = new Scanner(
                serviceUuid,
                slogan1Uuid,
                slogan2Uuid,
                slogan3Uuid,
                this
        );

        mHandler = new Handler();

        mHandler.postDelayed(() -> {
            mScanner.start();
            mAdvertiser.start();
        }, 100);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        w(TAG, "onDestroy called, destroying");
        mScanner.stop();
        mAdvertiser.stop();
        mHandler.getLooper().quitSafely();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
