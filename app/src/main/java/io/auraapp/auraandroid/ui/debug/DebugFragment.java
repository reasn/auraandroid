package io.auraapp.auraandroid.ui.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedState;
import io.auraapp.auraandroid.ui.common.ColorPicker;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;

import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_EXTRA_PEERS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_EXTRA_PEER;

public class DebugFragment extends ScreenFragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/permissions/" + DebugFragment.class.getSimpleName();
    private static final String characters = "📜📡💚😇abcdefghijklmnopqrstuvwxyz1234567890 ,.-öä#ü+!\"§$%&/()=?`";
    private final Handler mHandler = new Handler();
    private final Timer mTimer = new Timer(mHandler);
    private Timer.Timeout mRefreshTimeout;
    @Nullable
    private Set<Peer> mPeers;
    private CommunicatorState mState;
    private long mLastStateUpdateTimestamp;
    private long mLastIntentTimestamp;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.post(() -> {
                v(TAG, "onReceive, intent: %s", intent.getAction());
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }

                mLastIntentTimestamp = System.currentTimeMillis();

                if (INTENT_PEER_UPDATED_ACTION.equals(intent.getAction())) {
                    @SuppressWarnings("unchecked")
                    Peer peer = (Peer) extras.getSerializable(INTENT_PEER_UPDATED_EXTRA_PEER);
                    if (peer != null) {
                        if (mPeers == null) {
                            mPeers = new HashSet<>();
                        }
                        mPeers.remove(peer);
                        mPeers.add(peer);
                    }

                } else if (INTENT_PEER_LIST_UPDATED_ACTION.equals(intent.getAction())) {
                    @SuppressWarnings("unchecked")
                    Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS);
                    if (peers != null) {
                        mPeers = peers;
                    }
                }
                CommunicatorState state = (CommunicatorState) extras.getSerializable(IntentFactory.INTENT_COMMUNICATOR_EXTRA_STATE);
                if (state != null) {
                    // Intents only have state if it changed
                    mState = state;
                    mLastStateUpdateTimestamp = System.currentTimeMillis();
                }
                reflectState(context);
            });
        }
    };

    private String createRandomStringOfLength(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt((int) (Math.random() * characters.length())));
        }
        return result.toString();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.debug_fragment;
    }

    @Override
    protected void onResumeWithContext(MainActivity activity, ViewGroup rootView) {
        MyProfileManager profileManager = activity.getSharedServicesSet().mMyProfileManager;

        activity.registerReceiver(mReceiver, IntentFactory.communicatorIntentFilter());
        v(TAG, "Receiver registered");

        SharedState state = activity.getSharedState();
        mPeers = state.mPeers;
        mState = state.mCommunicatorState;
        mLastStateUpdateTimestamp = System.currentTimeMillis();

        // TODO come up with great personas

        rootView.findViewById(R.id.demo_0).setOnClickListener($ -> mHandler.post(() -> {
            profileManager.setName(createRandomStringOfLength(Config.PROFILE_NAME_MAX_LENGTH));

            StringBuilder text = new StringBuilder();
            for (int i = 0; i < Config.PROFILE_TEXT_MAX_LINE_BREAKS; i++) {
                text.append(createRandomStringOfLength(Config.PROFILE_TEXT_MAX_LENGTH / Config.PROFILE_TEXT_MAX_LINE_BREAKS)).append("\n");
            }
            profileManager.setText(text.toString());
            profileManager.setColor(new ColorPicker.SelectedColor("#ff00ff", 0, 0));
            profileManager.dropAllSlogans();
            for (int i = 0; i < Config.PROFILE_SLOGANS_MAX_SLOGANS; i++) {
                profileManager.adopt(Slogan.create(createRandomStringOfLength(Config.PROFILE_SLOGANS_MAX_LENGTH)));
            }
        }));
        rootView.findViewById(R.id.demo_1).setOnClickListener($ -> mHandler.post(() -> {
            profileManager.setName("Anonymous");
            profileManager.setText(EmojiHelper.replaceShortCode(":fire::fire::fire:\nDemocracy prevails. Let your kindness be a symbol for humanism and a better future"));
            profileManager.setColor(new ColorPicker.SelectedColor("#000000", 0, 0));
            profileManager.dropAllSlogans();
            profileManager.adopt(Slogan.create("Death to the dictator!"));
        }));
        rootView.findViewById(R.id.demo_1).setOnClickListener($ -> mHandler.post(() -> {
            profileManager.setName("Clara");
            profileManager.setText(EmojiHelper.replaceShortCode("My brother wants to collect"));
            profileManager.setColor(new ColorPicker.SelectedColor("#00ff00", 0, 0));
            profileManager.dropAllSlogans();
            profileManager.adopt(Slogan.create("Hello Moto"));
        }));

        Timer.clear(mRefreshTimeout);
        mRefreshTimeout = mTimer.setSerializedInterval(() -> mHandler.post(() -> reflectState(activity)), 1000);
        reflectState(activity);
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        Timer.clear(mRefreshTimeout);
        activity.unregisterReceiver(mReceiver);
        v(TAG, "Receiver unregistered");
    }

    private void reflectState(Context context) {
        long now = System.currentTimeMillis();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String dump = "\nlast communicator intent: " +
                (mLastIntentTimestamp > 0
                        ? (now - mLastIntentTimestamp) / 1000 + "s ago"
                        : "never");
        dump += "\nlast communicator state: " + (now - mLastStateUpdateTimestamp) / 1000 + "s ago";
        dump += "\ncommunicator: " + gson.toJson(mState);
        dump += "\npeers: " + gson.toJson(mPeers);
        TextView communicatorStateDump = getRootView().findViewById(R.id.debug_communicator_state_dump);
        communicatorStateDump.setText(dump.replaceAll("\"", "").replaceAll("\n +\\{", " {"));

        ListView peersList = getRootView().findViewById(R.id.debug_peers_list);

        if (mPeers == null) {
            peersList.setVisibility(View.GONE);
            return;
        }

        peersList.setVisibility(View.VISIBLE);
        // Not efficient but minimal code. That's okay because it doesn't affect performance
        // for users
        peersList.setAdapter(new DebugPeersListArrayAdapter(
                context,
                android.R.layout.simple_list_item_1,
                mPeers.toArray(new Peer[mPeers.size()])
        ));
    }
}
