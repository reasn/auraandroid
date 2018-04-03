package io.auraapp.auraandroid.ui.common;

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
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfile;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_COMMUNICATOR_STATE_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_EXTRA_PEERS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_EXTRA_PEER;

public class CommunicatorProxy {
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
    public interface PeerSetChangedCallback {
        void onPeerSetChanged(Set<Peer> peers);
    }

    @FunctionalInterface
    public interface PeerChangedCallback {
        void onPeerChanged(Peer peer);
    }

    public CommunicatorProxy(Context context,
                             PeerSetChangedCallback peerSetChangedCallback,
                             PeerChangedCallback peerChangedCallback,
                             StateUpdatedCallback stateUpdatedCallback) {
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

                if (INTENT_PEER_UPDATED_ACTION.equals(intent.getAction())) {

                    @SuppressWarnings("unchecked")
                    Peer peer = (Peer) extras.getSerializable(INTENT_PEER_UPDATED_EXTRA_PEER);
                    if (peer != null) {
                        v(TAG, "Peer updated, peer: %s, slogans: %d", peer.mId, peer.mSlogans.size());
                        peerChangedCallback.onPeerChanged(peer);
                    } else {
                        w(TAG, "Received invalid %s intent, peer: null", INTENT_PEER_UPDATED_ACTION);
                    }
                    // INTENT_PEER_UPDATED_ACTION is sent quite often and therefore not accompanied by
                    // communicator state. Return here to not generate warnings about the missing state
                    return;

                } else if (INTENT_PEER_LIST_UPDATED_ACTION.equals(intent.getAction())) {
                    @SuppressWarnings("unchecked")
                    Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS);

                    if (peers != null) {
                        v(TAG, "Peer list changed, (%d) peers: %s", peers.size(), peers);
                        peerSetChangedCallback.onPeerSetChanged(peers);
                    } else {
                        w(TAG, "Received invalid %s intent, peers: null, intent: %s", INTENT_PEER_LIST_UPDATED_ACTION, intent);
                    }

                } else if (!INTENT_COMMUNICATOR_STATE_UPDATED_ACTION.equals(intent.getAction())) {
                    w(TAG, "Received invalid intent (unknown action \"%s\"), ignoring it", intent.getAction());
                    return;
                }

                CommunicatorState state = (CommunicatorState) extras.getSerializable(IntentFactory.INTENT_COMMUNICATOR_EXTRA_STATE);


                if (state == null) {
                    w(TAG, "No state returned by communicator, intent: %s", intent);
                    return;
                }

                boolean stateChanged = !state.equals(mState);

                mState = state;
                if (stateChanged) {
                    i(TAG, "Communicator state changed, state: %s", state);
                    stateUpdatedCallback.onStateUpdated(state);
                }
            }
        };
    }

    public void startListening() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_COMMUNICATOR_STATE_UPDATED_ACTION);
        filter.addAction(INTENT_PEER_LIST_UPDATED_ACTION);
        filter.addAction(INTENT_PEER_UPDATED_ACTION);

        mContext.registerReceiver(mReceiver, filter);
        mRegistered = true;
        d(TAG, "Started to listen for events from communicator");
    }

    /**
     * @todo Tell communicator to stop sending intents until further notice
     */
    public void stopListening() {
        d(TAG, "Stopping to listen for events from communicator");
        if (mRegistered) {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    public void enable() {
        d(TAG, "Enabling communicator");
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_ENABLE_ACTION);
        mContext.startService(intent);
    }

    public void disable() {
        d(TAG, "Disabling communicator");
        if (!mState.mShouldCommunicate) {
            w(TAG, "Attempting to disable apparently already disabled communicator");
        }
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_DISABLE_ACTION);
        mContext.startService(intent);
    }

    public void askForPeersUpdate() {
        v(TAG, "Asking for peers update");
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_REQUEST_PEERS_ACTION);
        mContext.startService(intent);
    }

    public void updateMyProfile(MyProfile myProfile) {
        if (mState == null || !mState.mShouldCommunicate) {
            return;
        }

        v(TAG, "Sending profile to communicator, color: %s, slogans: %d", myProfile.getColor(), myProfile.getSlogans().size());

        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_MY_SLOGANS_CHANGED_ACTION);
//
//        String[] mySloganStrings = new String[myProfile.mSlogans.size()];
//        int index = 0;
//        for (Slogan slogan : myProfile.mSlogans) {
//            mySloganStrings[index++] = slogan.getText();
//        }
//        intent.putExtra(IntentFactory.INTENT_MY_SLOGANS_CHANGED_EXTRA_SLOGANS, mySloganStrings);
        intent.putExtra(IntentFactory.INTENT_MY_SLOGANS_CHANGED_EXTRA_PROFILE, myProfile);

        mContext.startService(intent);
    }
}
