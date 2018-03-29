package io.auraapp.auraandroid.ui.debug;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class DebugFragment extends Fragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/permissions/" + DebugFragment.class.getSimpleName();
    private Context mContext;
    private TextView mDebugCommunicatorStateDumpView;
    private ListView mDebugPeersListView;

    public static DebugFragment create(Context context) {
        DebugFragment fragment = new DebugFragment();
        fragment.mContext = context;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");

        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.debug_fragment, container, false);


        mDebugCommunicatorStateDumpView = rootView.findViewById(R.id.debug_communicator_state_dump);
        mDebugPeersListView = rootView.findViewById(R.id.debug_peers_list);
        return rootView;
    }

    public void update(@Nullable CommunicatorState communicatorState, @Nullable Set<Peer> peers) {

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
                mContext,
                android.R.layout.simple_list_item_1,
                peers.toArray(new Peer[peers.size()])
        ));
    }
}
