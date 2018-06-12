package io.auraapp.auraandroid.ui.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.Communicator.Communicator;
import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.common.AuraPrefs;
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
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_EXTRA_PROFILE;

public class CommunicatorProxy {
    private static final String TAG = "@aura/communicatorProxy";

    private final CommunicatorProxyState mState = new CommunicatorProxyState(null);
    private Set<Peer> mPeers = new HashSet<>();
    private boolean mRegistered = false;
    private final Context mContext;
    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMyProfile(
                    (MyProfile) intent.getSerializableExtra(LOCAL_MY_PROFILE_EXTRA_PROFILE)
            );
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
                    replacePeer(mPeers, peer);
                    v(TAG, "Peer updated, peer: %s, slogans: %d, peers: %d", peer.mId, peer.mSlogans.size(), mPeers.size());
                } else {
                    w(TAG, "Received invalid %s intent, peer: null", INTENT_PEER_UPDATED_ACTION);
                }
                // INTENT_PEER_UPDATED_ACTION is sent quite often and therefore not accompanied by
                // communicator state. Return here to not generate warnings about the missing state
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                return;

            } else if (INTENT_PEER_LIST_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS);

                if (peers != null) {
                    v(TAG, "Peer list changed, was %d, is: %s", mPeers.size(), peers.size());
                    mPeers = peers;
                } else {
                    w(TAG, "Received invalid %s intent, peers: null, intent: %s", INTENT_PEER_LIST_UPDATED_ACTION, intent);
                }
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

            } else if (!INTENT_COMMUNICATOR_STATE_UPDATED_ACTION.equals(intent.getAction())) {
                w(TAG, "Received invalid intent (unknown action \"%s\"), ignoring it", intent.getAction());
                return;
            }

            CommunicatorState state = (CommunicatorState) extras.getSerializable(IntentFactory.INTENT_COMMUNICATOR_EXTRA_STATE);

            if (state == null) {
                w(TAG, "No state returned by communicator, intent: %s", intent);
                return;
            }

            boolean stateChanged = !state.equals(mState.mCommunicatorState);

            mState.mCommunicatorState = state;
            if (stateChanged) {
                i(TAG, "Communicator state changed, state: %s", state);
                Intent stateUpdate = new Intent(LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION);
                stateUpdate.putExtra(LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE, mState);
                LocalBroadcastManager.getInstance(context).sendBroadcast(stateUpdate);
            }
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    };

    public CommunicatorProxy(Context context) {
        mContext = context;

        if (AuraPrefs.isEnabled(context)) {
            enable();
        } else {
            i(TAG, "Aura is currently disabled");
        }
    }

    public static void replacePeer(Set<Peer> mutablePeers, Peer peer) {
        for (Peer candidate : mutablePeers.toArray(new Peer[mutablePeers.size()])) {
            if (candidate.mId == peer.mId) {
                mutablePeers.remove(candidate);
            }
        }
        mutablePeers.add(peer);
    }

    public static Set<Peer> getPeersWithName(Set<Peer> peers) {
        Set<Peer> peersWithName = new HashSet<>();
        for (Peer peer : peers) {
            if (peer.mName != null) {
                peersWithName.add(peer);
            }
        }
        return peersWithName;
    }

    public void resume() {

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mLocalReceiver,
                IntentFactory.localMyProfileChangedIntentFiler()
        );

        mContext.registerReceiver(mReceiver, IntentFactory.createFilter(
                INTENT_COMMUNICATOR_STATE_UPDATED_ACTION,
                INTENT_PEER_LIST_UPDATED_ACTION,
                INTENT_PEER_UPDATED_ACTION
        ));
        mRegistered = true;
        d(TAG, "Started to listen for events from communicator");
    }

    /**
     * @todo Tell communicator to stop sending intents until further notice
     */
    public void pause() {
        d(TAG, "Stopping to listen for events from communicator");
        if (mRegistered) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mLocalReceiver);
            mContext.unregisterReceiver(mReceiver);
        }
    }

    public void enable() {
        d(TAG, "Enabling communicator");
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_ENABLE_ACTION);
        mState.mEnabled = true;
        AuraPrefs.putEnabled(mContext, true);
        mContext.startService(intent);
    }

    public void updateMyProfile(MyProfile myProfile) {
        if (!mState.mEnabled) {
            return;
        }

        v(TAG, "Sending profile to communicator, color: %s, slogans: %d", myProfile.getColor(), myProfile.getSlogans().size());

        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_MY_PROFILE_CHANGED_ACTION);
        intent.putExtra(IntentFactory.INTENT_MY_PROFILE_CHANGED_EXTRA_PROFILE, myProfile);

        mContext.startService(intent);
    }

    public void disable() {
        d(TAG, "Disabling communicator");
        if (!mState.mEnabled) {
            w(TAG, "Attempting to disable apparently already disabled communicator");
        }
        mState.mEnabled = false;
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_DISABLE_ACTION);
        AuraPrefs.putEnabled(mContext, false);
        mContext.startService(intent);
    }

    public void askForPeersUpdate() {
        v(TAG, "Asking for peers update");
        Intent intent = new Intent(mContext, Communicator.class);
        intent.setAction(IntentFactory.INTENT_REQUEST_PEERS_ACTION);
        mContext.startService(intent);
    }

    public CommunicatorProxyState getState() {
        return mState;
    }

    public Set<Peer> getPeers() {
        return mPeers;
    }
}
