package io.auraapp.auraandroid.common;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.Serializable;
import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;

public class IntentFactory {

    private static final String prefix = "io.auraapp.aura.";

    // to Communicator
    public static final String INTENT_ENABLE_ACTION = prefix + "enableCommunicator";
    public static final String INTENT_DISABLE_ACTION = prefix + "disableCommunicator";
    public static final String INTENT_REQUEST_PEERS_ACTION = prefix + "requestPeers";
    public static final String INTENT_MY_PROFILE_CHANGED_ACTION = prefix + "mySlogansChanged";
    public static final String INTENT_MY_SLOGANS_CHANGED_EXTRA_SLOGANS = prefix + "mySlogansExtra";
    public static final String INTENT_MY_PROFILE_CHANGED_EXTRA_PROFILE = prefix + "myProfileExtra";

    // from Communicator
    public static final String INTENT_COMMUNICATOR_EXTRA_STATE = prefix + "extraState";

    public static final String INTENT_COMMUNICATOR_STATE_UPDATED_ACTION = prefix + "communicatorStateUpdated";

    public static final String INTENT_PEER_LIST_UPDATED_ACTION = prefix + "peerListUpdated";
    public static final String INTENT_PEER_LIST_UPDATED_EXTRA_PEERS = prefix + "extraPeerList";

    public static final String INTENT_PEER_UPDATED_ACTION = prefix + "peerUpdated";
    public static final String INTENT_PEER_UPDATED_EXTRA_PEER = prefix + "extraPeer";

    // local (activity)
    public static final String LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION = prefix + "local.communicatorState";
    public static final String LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE = prefix + "local.extraProxyState";

    public final static String LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION = prefix + "local.myProfile.colorChanged";
    public final static String LOCAL_MY_PROFILE_NAME_CHANGED_ACTION = prefix + "local.myProfile.nameChanged";
    public final static String LOCAL_MY_PROFILE_TEXT_CHANGED_ACTION = prefix + "local.myProfile.textChanged";
    public final static String LOCAL_MY_PROFILE_DROPPED_ACTION = prefix + "local.myProfile.sloganDropped";
    public final static String LOCAL_MY_PROFILE_ADOPTED_ACTION = prefix + "local.myProfile.sloganAdopted";
    public final static String LOCAL_MY_PROFILE_REPLACED_ACTION = prefix + "local.myProfile.sloganReplaced";
    public final static String LOCAL_MY_PROFILE_EXTRA_PROFILE = prefix + "local.myProfile.extraProfile";

    public static final String LOCAL_SCREEN_PAGER_CHANGED_ACTION = prefix + "local.screenPagerChanged";
    public static final String LOCAL_SCREEN_PAGER_CHANGED_EXTRA_PREVIOUS = prefix + "local.screenPagerChanges.extraPrevious";
    public static final String LOCAL_SCREEN_PAGER_CHANGED_EXTRA_NEW = prefix + "local.screenPagerChanges.extraNew";

    public static final String LOCAL_TUTORIAL_OPENED_ACTION = prefix + "local.tutorial.opened";
    public static final String LOCAL_TUTORIAL_COMPLETED_ACTION = prefix + "local.tutorial.completed";
    public static final String PREFERENCE_CHANGED_ACTION = prefix + "preference.changed";
    public static final String PREFERENCE_CHANGED_EXTRA_KEY = prefix + "preference.changed.extraKey";
    public static final String PREFERENCE_CHANGED_EXTRA_VALUE = prefix + "preference.changed.extraValue";

    public static IntentFilter localMyProfileChangedIntentFiler() {
        return createFilter(
                LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION,
                LOCAL_MY_PROFILE_NAME_CHANGED_ACTION,
                LOCAL_MY_PROFILE_TEXT_CHANGED_ACTION,
                LOCAL_MY_PROFILE_DROPPED_ACTION,
                LOCAL_MY_PROFILE_ADOPTED_ACTION,
                LOCAL_MY_PROFILE_REPLACED_ACTION
        );
    }

    public static IntentFilter createFilter(String... actions) {
        IntentFilter filter = new IntentFilter();
        for (String action : actions) {
            filter.addAction(action);
        }
        return filter;
    }

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
