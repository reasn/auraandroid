package io.auraapp.auraandroid.ui.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
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
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
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
    @Nullable
    private Set<Peer> mPeers = new HashSet<>();
    private Runnable mDemo0ClickListener;
    private Runnable mDemo1ClickListener;
    private Runnable mDemo2ClickListener;
    private CommunicatorState mState;

    private ViewGroup mRootView;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context $, Intent intent) {
            v(TAG, "onReceive, intent: %s", intent.getAction());
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }

            if (INTENT_PEER_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Peer peer = (Peer) extras.getSerializable(INTENT_PEER_UPDATED_EXTRA_PEER);
                if (peer != null) {
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
            }
            reflectState();
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
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }

        MyProfileManager profileManager = ((MainActivity) context).getSharedServicesSet().mMyProfileManager;

        mDemo0ClickListener = () -> {
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
        };

        // TODO come up with great personas
        mDemo1ClickListener = () -> {
            profileManager.setName("Anonymous");
            profileManager.setText(EmojiHelper.replaceShortCode(":fire::fire::fire:\nDemocracy prevails. Let your kindness be a symbol for humanism and a better future"));
            profileManager.setColor(new ColorPicker.SelectedColor("#000000", 0, 0));
            profileManager.dropAllSlogans();
            profileManager.adopt(Slogan.create("Death to the dictator!"));
        };

        mDemo2ClickListener = () -> {
            profileManager.setName("Clara");
            profileManager.setText(EmojiHelper.replaceShortCode("My brother wants to collect"));
            profileManager.setColor(new ColorPicker.SelectedColor("#00ff00", 0, 0));
            profileManager.dropAllSlogans();
            profileManager.adopt(Slogan.create("Hello Moto"));
        };
    }

    @Override
    @ExternalInvocation
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        mRootView = (ViewGroup) inflater.inflate(
                R.layout.debug_fragment, container, false);

        mRootView.findViewById(R.id.demo_0).setOnClickListener($ -> mHandler.post(mDemo0ClickListener));
        mRootView.findViewById(R.id.demo_1).setOnClickListener($ -> mHandler.post(mDemo1ClickListener));
        mRootView.findViewById(R.id.demo_1).setOnClickListener($ -> mHandler.post(mDemo2ClickListener));

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            getContext().registerReceiver(mReceiver, IntentFactory.communicatorIntentFilter());
            v(TAG, "Receiver registered");

            SharedState state = ((MainActivity) getContext()).getSharedState();
            mPeers = state.mPeers;
            mState = state.mCommunicatorState;

        }
        reflectState();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            getContext().unregisterReceiver(mReceiver);
            v(TAG, "Receiver unregistered");
        }
    }

    private void reflectState() {

        if (mRootView == null || getContext() == null) {
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String dump = "# communicator: " + gson.toJson(mState);
        dump += "\n# peers: " + gson.toJson(mPeers);
        TextView communicatorStateDump = mRootView.findViewById(R.id.debug_communicator_state_dump);
        communicatorStateDump.setText(dump.replaceAll("\"", "").replaceAll("\n +\\{", " {"));

        ListView peersList = mRootView.findViewById(R.id.debug_peers_list);

        if (mPeers == null) {
            peersList.setVisibility(View.GONE);
            return;
        }

        peersList.setVisibility(View.VISIBLE);
        // Not efficient but minimal code. That's okay because it doesn't affect performance
        // for users
        peersList.setAdapter(new DebugPeersListArrayAdapter(
                getContext(),
                android.R.layout.simple_list_item_1,
                mPeers.toArray(new Peer[mPeers.size()])
        ));
    }
}
