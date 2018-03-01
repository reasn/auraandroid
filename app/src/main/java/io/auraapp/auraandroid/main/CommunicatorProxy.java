package io.auraapp.auraandroid.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.util.Set;

import io.auraapp.auraandroid.Communicator.Communicator;
import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_COMMUNICATOR_STATE_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEERS_UPDATE_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEERS_UPDATE_EXTRA_PEERS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_SEEN_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_SEEN_EXTRA_ADDRESS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_SEEN_EXTRA_TIMESTAMP;

class CommunicatorProxy {
    private static final String TAG = "@aura/communicatorProxy";

    private CommunicatorState mState = null;

    private final Context mContext;

    private final BroadcastReceiver mReceiver;
    private boolean mRegistered = false;

    @FunctionalInterface
    public interface StateUpdatedCallback {
        void onStateUpdated(CommunicatorState state);
    }

    @FunctionalInterface
    public interface PeersChangedCallback {
        void onPeersChanged(Set<Peer> peers);
    }

    @FunctionalInterface
    public interface PeerSeenCallback {
        void onPeerSeen(String address, long timestamp);
    }

    CommunicatorProxy(Context context, PeersChangedCallback peersChangedCallback, PeerSeenCallback peerSeenCallback, StateUpdatedCallback stateUpdatedCallback) {
        mContext = context;

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context $, Intent intent) {
                v(TAG, "onReceive, intent: %s", intent.getAction());
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    w(TAG, "Received invalid intent (extras are null), ignoring it");
                    return;
                }

                if (INTENT_PEER_SEEN_ACTION.equals(intent.getAction())) {
                    String address = extras.getString(INTENT_PEER_SEEN_EXTRA_ADDRESS);
                    long timestamp = extras.getLong(INTENT_PEER_SEEN_EXTRA_TIMESTAMP);
                    if (address == null || timestamp < 1) {
                        w(TAG, "Received invalid %s intent, address: %s, timestamp: %d", INTENT_PEER_SEEN_ACTION, address, timestamp);
                    } else {
                        peerSeenCallback.onPeerSeen(address, timestamp);
                    }
                    // INTENT_PEER_SEEN_ACTION is sent quite often and therefore not accompanied by
                    // communicator state. Return here to not generate warnings about the missing state
                    return;

                } else if (INTENT_PEERS_UPDATE_ACTION.equals(intent.getAction())) {

                    @SuppressWarnings("unchecked")
                    Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEERS_UPDATE_EXTRA_PEERS);

                    if (peers != null) {
                        peersChangedCallback.onPeersChanged(peers);
                    } else {
                        w(TAG, "Received invalid %s intent, peers: null, intent: %s", INTENT_PEERS_UPDATE_ACTION, intent);
                    }

                } else if (!INTENT_COMMUNICATOR_STATE_UPDATED_ACTION.equals(intent.getAction())) {
                    w(TAG, "Received invalid intent (unknown action \"%s\"), ignoring it", intent.getAction());
                    return;
                }

                CommunicatorState state = (CommunicatorState) extras.getSerializable(IntentFactory.INTENT_COMMUNICATOR_EXTRA_STATE);

                w(TAG, "State %s", state);

                if (state == null) {
                    w(TAG, "No state returned by communicator");
                    return;
                }

                boolean stateChanged = !state.equals(mState);

                mState = state;
                if (stateChanged) {
                    stateUpdatedCallback.onStateUpdated(state);
                }
            }
        };
    }

    void startListening() {
        d(TAG, "Starting to listen for events from communicator");
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_COMMUNICATOR_STATE_UPDATED_ACTION);
        filter.addAction(INTENT_PEERS_UPDATE_ACTION);

        mContext.registerReceiver(mReceiver, filter);
        mRegistered = true;
    }

    /**
     * @todo Tell communicator to stop sending intents until further notice
     */
    void stopListening() {
        d(TAG, "Stopping to listen for events from communicator");
        if (mRegistered) {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    void enable() {
        d(TAG, "Enabling communicator");
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_ENABLE_ACTION);
        mContext.startService(intent);
    }

    void disable() {
        d(TAG, "Disabling communicator");
        if (!mState.mShouldCommunicate) {
            w(TAG, "Attempting to disable apparently already disabled communicator");
        }
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_DISABLE_ACTION);
        mContext.startService(intent);
    }

    void askForPeersUpdate() {
        v(TAG, "Asking for peers update");
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_REQUEST_PEERS_ACTION);
        mContext.startService(intent);
    }

    void updateMySlogans(Set<Slogan> slogans) {
        v(TAG, "Sending %d slogans to communicator", slogans.size());

        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_MY_SLOGANS_CHANGED_ACTION);

        String[] mySloganStrings = new String[slogans.size()];
        int index = 0;
        for (Slogan slogan : slogans) {
            mySloganStrings[index++] = slogan.getText();
        }
        intent.putExtra(IntentFactory.INTENT_MY_SLOGANS_CHANGED_EXTRA_SLOGANS, mySloganStrings);

        mContext.startService(intent);
    }
}
