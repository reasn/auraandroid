package io.auraapp.auraandroid.ui.debug;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.common.ColorPicker;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class DebugFragment extends ScreenFragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/permissions/" + DebugFragment.class.getSimpleName();
    private final Handler mHandler = new Handler();

    private TextView mDebugCommunicatorStateDumpView;
    private ListView mDebugPeersListView;
    private boolean mHasView = false;
    private MyProfileManager mMyProfileManager;

    public static DebugFragment create(Context context, MyProfileManager myProfileManager) {
        DebugFragment fragment = new DebugFragment();
        fragment.setContext(context);
        fragment.mMyProfileManager = myProfileManager;
        return fragment;
    }

    private static final String characters = "📜📡💚😇abcdefghijklmnopqrstuvwxyz1234567890 ,.-öä#ü+!\"§$%&/()=?`";

    private String createRandomStringOfLength(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt((int) (Math.random() * characters.length())));
        }
        return result.toString();
    }

    @Override
    @ExternalInvocation
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.debug_fragment, container, false);

        mDebugCommunicatorStateDumpView = rootView.findViewById(R.id.debug_communicator_state_dump);
        mDebugPeersListView = rootView.findViewById(R.id.debug_peers_list);
        mHasView = true;

        rootView.findViewById(R.id.demo_0).setOnClickListener($ -> mHandler.post(() -> {
            mMyProfileManager.setName(createRandomStringOfLength(Config.PROFILE_NAME_MAX_LENGTH));

            StringBuilder text = new StringBuilder();
            for (int i = 0; i < Config.PROFILE_TEXT_MAX_LINE_BREAKS; i++) {
                text.append(createRandomStringOfLength(Config.PROFILE_TEXT_MAX_LENGTH / Config.PROFILE_TEXT_MAX_LINE_BREAKS)).append("\n");
            }
            mMyProfileManager.setText(text.toString());
            mMyProfileManager.setColor(new ColorPicker.SelectedColor("#ff00ff", 0, 0));
            mMyProfileManager.dropAllSlogans();
            for (int i = 0; i < Config.PROFILE_SLOGANS_MAX_SLOGANS; i++) {
                mMyProfileManager.adopt(Slogan.create(createRandomStringOfLength(Config.PROFILE_SLOGANS_MAX_LENGTH)));
            }
        }));

        // TODO come up with great personas
        rootView.findViewById(R.id.demo_1).setOnClickListener($ -> mHandler.post(() -> {
            mMyProfileManager.setName("Anonymous");
            mMyProfileManager.setText(EmojiHelper.replaceShortCode(":fire::fire::fire:\nDemocracy prevails. Let your kindness be a symbol for humanism and a better future"));
            mMyProfileManager.setColor(new ColorPicker.SelectedColor("#000000", 0, 0));
            mMyProfileManager.dropAllSlogans();
            mMyProfileManager.adopt(Slogan.create("Death to the dictator!"));
        }));

        rootView.findViewById(R.id.demo_2).setOnClickListener($ -> mHandler.post(() -> {
            mMyProfileManager.setName("Clara");
            mMyProfileManager.setText(EmojiHelper.replaceShortCode("My brother wants to collect"));
            mMyProfileManager.setColor(new ColorPicker.SelectedColor("#00ff00", 0, 0));
            mMyProfileManager.dropAllSlogans();
            mMyProfileManager.adopt(Slogan.create("Hello Moto"));
        }));

        return rootView;
    }

    public void update(@Nullable CommunicatorState communicatorState, @Nullable Set<Peer> peers) {
        if (!mHasView) {
            return;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String dump = "# communicator: " + gson.toJson(communicatorState);
        dump += "\n# peers: " + gson.toJson(peers);

        mDebugCommunicatorStateDumpView.setText(dump.replaceAll("\"", "").replaceAll("\n +\\{", " {"));
        if (peers == null) {
            mDebugPeersListView.setVisibility(View.GONE);
            return;
        }

        mDebugPeersListView.setVisibility(View.VISIBLE);
        // Not efficient but minimal code. That's okay because it doesn't affect performance
        // for users
        mDebugPeersListView.setAdapter(new DebugPeersListArrayAdapter(
                getContext(),
                android.R.layout.simple_list_item_1,
                peers.toArray(new Peer[peers.size()])
        ));
    }
}
