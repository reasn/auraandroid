package io.auraapp.auraandroid.ui.world;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.SharedState;
import io.auraapp.auraandroid.ui.common.CommunicatorStateRenderer;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.world.list.OnAdoptCallback;
import io.auraapp.auraandroid.ui.world.list.PeersRecycleAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_EXTRA_PEERS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_EXTRA_PEER;

public class WorldFragment extends ScreenFragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/world/fragment";
    private ViewGroup mRootView;
    private InfoBox mCommunicatorStateInfoBox;
    private TextView mStatusSummary;
    private Handler mHandler = new Handler();
    private OnAdoptCallback mOnAdoptCallback;
    private PeersRecycleAdapter mPeerListAdapter;
    private FakeSwipeRefreshLayout mSwipeRefresh;

    private CommunicatorState mCommunicatorState = null;
    private Set<Peer> mPeers = new HashSet<>();
    private InfoBox mPeersInfoBox;

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
                    if (mPeerListAdapter != null) {
                        mPeerListAdapter.notifyPeersChanged(mPeers);
                    }
                }

            } else if (INTENT_PEER_LIST_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS);
                if (peers != null) {
                    mPeers = peers;
                    if (mPeerListAdapter != null) {
                        mPeerListAdapter.notifyPeersChanged(mPeers);
                    }
                }
            }

            CommunicatorState state = (CommunicatorState) extras.getSerializable(IntentFactory.INTENT_COMMUNICATOR_EXTRA_STATE);
            if (state != null) {
                // Intents only have state if it changed
                mCommunicatorState = state;
            }
            reflectState();
        }
    };
    private RecyclerView mPeersRecycler;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }

        SharedServicesSet servicesSet = ((MainActivity) context).getSharedServicesSet();

        mOnAdoptCallback = slogan -> {
            if (servicesSet.mMyProfileManager.getProfile().getSlogans().contains(slogan)) {
                toast(R.string.ui_world_toast_slogan_already_adopted);
            } else if (servicesSet.mMyProfileManager.spaceAvailable()) {
                servicesSet.mMyProfileManager.adopt(slogan);
            } else {
                servicesSet.mDialogManager.showReplace(
                        servicesSet.mMyProfileManager.getProfile().getSlogans(),
                        sloganToReplace -> servicesSet.mMyProfileManager.replace(sloganToReplace, slogan)
                );
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.world_fragment, container, false);

        mStatusSummary = mRootView.findViewById(R.id.profile_status_summary);
        mCommunicatorStateInfoBox = mRootView.findViewById(R.id.profile_status_info_box);
        mPeersInfoBox = mRootView.findViewById(R.id.peer_slogans_info_box);

        mSwipeRefresh = mRootView.findViewById(R.id.fake_swipe_to_refresh);
        mSwipeRefresh.setEnabled(false);

        return mRootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPeersRecycler = mRootView.findViewById(R.id.profile_slogans_recycler);
        mPeerListAdapter = new PeersRecycleAdapter(getContext(), mPeersRecycler, mOnAdoptCallback);
        mPeersRecycler.setAdapter(mPeerListAdapter);
        mPeersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mPeersRecycler.setNestedScrollingEnabled(false);
        // With change animations enabled items flash as updates come in
        ((SimpleItemAnimator) mPeersRecycler.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            getContext().registerReceiver(mReceiver, IntentFactory.communicatorIntentFilter());
            v(TAG, "Receiver registered");

            SharedState state = ((MainActivity) getContext()).getSharedState();
            mPeers = state.mPeers;
            mCommunicatorState = state.mCommunicatorState;

        }
        if (mPeerListAdapter != null) {
            // onResume might be called before view has been created (activity.create -> activity.resume -> this.onResume()
            mPeerListAdapter.onResume();
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
        if (mPeerListAdapter != null) {
            // onPause might be called before view has been created (activity.create -> activity.resume -> this.onResume()
            mPeerListAdapter.onPause();
        }
    }

    public void reflectState() {
        if (mSwipeRefresh != null) {
            // onCreateView might not have been called yet
            mSwipeRefresh.setEnabled(mCommunicatorState != null && mCommunicatorState.mScanning && mPeers != null && mPeers.size() > 0);
            mSwipeRefresh.setPeerCount(mPeers != null ? mPeers.size() : 0);
        }

        if (mCommunicatorStateInfoBox != null) {
            // onCreateView might not have been called yet
            CommunicatorStateRenderer.populateInfoBoxWithState(
                    mCommunicatorState,
                    mCommunicatorStateInfoBox,
                    mStatusSummary,
                    getContext());
            mPeersRecycler.setVisibility(mCommunicatorState != null && mCommunicatorState.mShouldCommunicate
                    ? View.VISIBLE
                    : View.GONE
            );
            updatePeersInfoBox();
        }


//        updatePeersHeading();
    }

//    private void updatePeersHeading() {
//
//        View heading = mRootView.findViewById(R.id.peer_slogans_heading_wrapper);
//
//        // Don't show heading if any info boxes are visible
//        if (mCommunicatorStateInfoBox.getVisibility() == View.VISIBLE
//                || mPeersInfoBox.getVisibility() == View.VISIBLE) {
//            heading.setVisibility(View.GONE);
//            return;
//
//        }
//        String headerText = getContext().getResources().getQuantityString(R.plurals.ui_world_peers_heading_slogans, mLastPeerSloganMap.size(), mLastPeerSloganMap.size());
//
//        boolean synchronizing = false;
//        for (Peer peer : mPeers) {
//            if (peer.mSynchronizing) {
//                synchronizing = true;
//                break;
//            }
//        }
//        mRootView.findViewById(R.id.peer_slogans_heading_progress_bar).setVisibility(
//                synchronizing
//                        ? View.VISIBLE
//                        : View.GONE
//        );
//
//        ((TextView) mRootView.findViewById(R.id.peer_slogans_heading_text)).setText(EmojiHelper.replaceShortCode(headerText));
//
//        heading.setVisibility(View.VISIBLE);
//    }

    private void updatePeersInfoBox() {

        // Show mPeersInfoBox only if there's no communicator state info box visible and there's no peers
        if (mCommunicatorStateInfoBox.getVisibility() == View.VISIBLE || mPeers.size() > 0) {
            mPeersInfoBox.setVisibility(View.GONE);
            return;
        }

//        int nearbyPeers = 0;
//        long now = System.currentTimeMillis();
//        for (Peer peer : peers) {
//            if (now - peer.mLastSeenTimestamp < 30000) {
//                nearbyPeers++;
//            }
//        }

        if (System.currentTimeMillis() - mCommunicatorState.mScanStartTimestamp < Config.MAIN_LOOKING_AROUND_SHOW_DURATION) {
            mPeersInfoBox.setHeading(R.string.ui_world_starting_heading);
            mPeersInfoBox.setText(R.string.ui_world_starting_text);
            mPeersInfoBox.setEmoji(":satellite_antenna:");
            mPeersInfoBox.hideButton();

        } else {
            mPeersInfoBox.setHeading(R.string.ui_world_no_peers_info_heading);
            mPeersInfoBox.setText(R.string.ui_world_no_peers_info_text);
            mPeersInfoBox.setEmoji(":see_no_evil:");
            mPeersInfoBox.showButton(
                    R.string.ui_world_no_peers_info_heading_cta,
                    R.string.ui_world_no_peers_info_second_text,
                    $ -> {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(
                                Intent.EXTRA_TEXT,
                                EmojiHelper.replaceShortCode(getContext().getString(R.string.ui_main_share_text))
                        );
                        sendIntent.setType("text/plain");
                        getContext().startActivity(sendIntent);
                    });
        }

        mPeersInfoBox.setBackgroundColor(getContext().getResources().getColor(R.color.infoBoxWarning));
        mPeersInfoBox.setVisibility(View.VISIBLE);
    }
}
