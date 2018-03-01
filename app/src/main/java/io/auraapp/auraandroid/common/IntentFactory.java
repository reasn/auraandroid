package io.auraapp.auraandroid.common;

import android.content.Context;
import android.content.Intent;

import java.io.Serializable;
import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;

public class IntentFactory {

    private static final String prefix = "io.auraapp.aura.";

    // to Communicator
    public static final String INTENT_ENABLE_ACTION = prefix + "enableCommunicator";
    public static final String INTENT_DISABLE_ACTION = prefix + "disableCommunicator";
    public static final String INTENT_REQUEST_PEERS_ACTION = prefix + "requestPeers";
    public static final String INTENT_MY_SLOGANS_CHANGED_ACTION = prefix + "mySlogansChanged";
    public static final String INTENT_MY_SLOGANS_CHANGED_EXTRA_SLOGANS = prefix + "mySlogansExtra";

    // from Communicator
    public static final String INTENT_COMMUNICATOR_STATE_UPDATED_ACTION = prefix + "healthUpdated";
    public static final String INTENT_PEERS_UPDATE_ACTION = prefix + "peersUpdated";
    public static final String INTENT_PEERS_UPDATE_EXTRA_PEERS = prefix + "extraPeers";
    public static final String INTENT_COMMUNICATOR_EXTRA_STATE = prefix + "extraState";

    public static final String INTENT_PEER_UPDATE_ACTION = prefix + "peerChanged";
    public static final String INTENT_PEER_UPDATE_EXTRA_PEER = prefix + "extraPeer";


    public static Intent peersUpdate(Set<Peer> peers, CommunicatorState state) {
        Intent intent = new Intent(IntentFactory.INTENT_PEERS_UPDATE_ACTION);
        intent.putExtra(IntentFactory.INTENT_PEERS_UPDATE_EXTRA_PEERS, (Serializable) peers);
        intent.putExtra(IntentFactory.INTENT_COMMUNICATOR_EXTRA_STATE, state);
        return intent;
    }

    public static Intent peerUpdate(Peer peer) {
        Intent intent = new Intent(IntentFactory.INTENT_PEER_UPDATE_ACTION);
        intent.putExtra(IntentFactory.INTENT_PEER_UPDATE_EXTRA_PEER, peer);
        return intent;
    }

    public static Intent showActivity(Context context, Class activity) {
        Intent intent = new Intent(context, activity);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent communicatorState(CommunicatorState state) {
        Intent intent = new Intent(IntentFactory.INTENT_COMMUNICATOR_STATE_UPDATED_ACTION);
        intent.putExtra(IntentFactory.INTENT_COMMUNICATOR_EXTRA_STATE, state);
        return intent;
    }
}
