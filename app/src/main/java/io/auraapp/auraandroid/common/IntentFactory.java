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
    public static final String INTENT_MY_SLOGANS_CHANGED_EXTRA_PROFILE = prefix + "myProfileExtra";

    // from Communicator
    public static final String INTENT_COMMUNICATOR_EXTRA_STATE = prefix + "extraState";

    public static final String INTENT_COMMUNICATOR_STATE_UPDATED_ACTION = prefix + "communicatorStateUpdated";

    public static final String INTENT_PEER_LIST_UPDATED_ACTION = prefix + "peerListUpdated";
    public static final String INTENT_PEER_LIST_UPDATED_EXTRA_PEERS = prefix + "extraPeerList";

    public static final String INTENT_PEER_UPDATED_ACTION = prefix + "peerUpdated";
    public static final String INTENT_PEER_UPDATED_EXTRA_PEER = prefix + "extraPeer";

    public static Intent peerListUpdated(Set<Peer> peers, CommunicatorState state) {
        Intent intent = new Intent(INTENT_PEER_LIST_UPDATED_ACTION);
        intent.putExtra(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS, (Serializable) peers);
        intent.putExtra(INTENT_COMMUNICATOR_EXTRA_STATE, state);
        return intent;
    }

    public static Intent peerUpdated(Peer peer) {
        Intent intent = new Intent(INTENT_PEER_UPDATED_ACTION);
        intent.putExtra(INTENT_PEER_UPDATED_EXTRA_PEER, peer);
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
        Intent intent = new Intent(INTENT_COMMUNICATOR_STATE_UPDATED_ACTION);
        intent.putExtra(INTENT_COMMUNICATOR_EXTRA_STATE, state);
        return intent;
    }
}
