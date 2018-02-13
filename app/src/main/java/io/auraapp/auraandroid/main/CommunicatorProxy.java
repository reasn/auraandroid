package io.auraapp.auraandroid.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.util.Set;

import io.auraapp.auraandroid.Communicator.Communicator;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

import static io.auraapp.auraandroid.Communicator.Communicator.INTENT_PEERS_UPDATE_PEERS_EXTRA;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;

class CommunicatorProxy {
    private static final String TAG = "@aura/" + CommunicatorProxy.class.getSimpleName();

    private int mHealth = Communicator.HEALTH_DOWN;

    private final Context mContext;

    private boolean mIsShuttingDown = false;
    private boolean mIsStarting = false;
    private BroadcastReceiver mReceiver;

    @FunctionalInterface
    public interface HealthChangeCallback {
        void onHealthChange(int health);
    }

    @FunctionalInterface
    public interface PeersUpdateCallback {
        void onPeersUpdate(Set<Peer> peers);
    }

    CommunicatorProxy(Context context, PeersUpdateCallback peersUpdateCallback, HealthChangeCallback healthChangeCallback) {
        mContext = context;

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context $, Intent intent) {

                Bundle extras = intent.getExtras();
                if (extras == null) {
                    w(TAG, "Received invalid intent (extras are null), ignoring it");
                    return;
                }
                int health = extras.getInt(Communicator.INTENT_COMMUNICATOR_HEALTH_EXTRA);

                if (Communicator.INTENT_PEERS_UPDATE_ACTION.equals(intent.getAction())) {

                    @SuppressWarnings("unchecked")
                    Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEERS_UPDATE_PEERS_EXTRA);

                    if (peers != null) {
                        peersUpdateCallback.onPeersUpdate(peers);
                    } else {
                        w(TAG, "Received invalid %s intent, peers: null, intent: %s", Communicator.INTENT_PEERS_UPDATE_ACTION, intent);
                    }

                } else if (!Communicator.INTENT_HEALTH_UPDATE_ACTION.equals(intent.getAction())) {
                    w(TAG, "Received invalid intent (unknown action \"%s\"), ignoring it", intent.getAction());
                    return;
                }

                boolean healthChanged = mHealth != health;
                mHealth = health;


                if (healthChanged) {
                    healthChangeCallback.onHealthChange(mHealth);
                }
            }
        };
    }

    void startListening() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(Communicator.INTENT_HEALTH_UPDATE_ACTION);
        filter.addAction(Communicator.INTENT_PEERS_UPDATE_ACTION);

        mContext.registerReceiver(mReceiver, filter);
    }

    /**
     * @todo Tell communicator to stop sending intents until further notice
     */
    void stopListening() {
        mContext.unregisterReceiver(mReceiver);
    }

    void enable() {
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(Communicator.INTENT_ENABLE_ACTION);
        mContext.startService(intent);
    }

    void disable() {
        if (mHealth == Communicator.HEALTH_DOWN) {
            w(TAG, "Attempting to disable apparently already disabled communicator");
        }
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(Communicator.INTENT_DISABLE_ACTION);
        mContext.startService(intent);
    }

    void askForPeerUpdate() {
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(Communicator.INTENT_REQUEST_PEERS_ACTION);
        mContext.startService(intent);
    }

    void updateMySlogans(Set<Slogan> slogans) {

        v(TAG, "Sending %d slogans to communicator", slogans.size());

        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(Communicator.INTENT_MY_SLOGANS_CHANGED_ACTION);

        String[] mySloganStrings = new String[slogans.size()];
        int index = 0;
        for (Slogan slogan : slogans) {
            mySloganStrings[index++] = slogan.getText();
        }
        intent.putExtra(Communicator.INTENT_MY_SLOGANS_CHANGED_SLOGANS_EXTRA, mySloganStrings);

        mContext.startService(intent);
    }
}
