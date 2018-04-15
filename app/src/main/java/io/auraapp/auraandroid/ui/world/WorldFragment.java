package io.auraapp.auraandroid.ui.world;

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

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.ActivityState;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.common.CommunicatorStateRenderer;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.world.list.OnAdoptCallback;
import io.auraapp.auraandroid.ui.world.list.PeersRecycleAdapter;

public class WorldFragment extends ScreenFragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/world/fragment";
    private ViewGroup mRootView;
    private InfoBox mCommunicatorStateInfoBox;
    private TextView mStatusSummary;
    private Handler mHandler = new Handler();
    private OnAdoptCallback mOnAdoptCallback;
    private PeersRecycleAdapter mPeerListAdapter;
    private FakeSwipeRefreshLayout mSwipeRefresh;

    private CommunicatorState mLastState = null;
    private TreeMap<String, PeerSlogan> mLastPeerSloganMap = null;
    private Set<Peer> mLastPeers = null;
    private InfoBox mPeersInfoBox;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }

        ActivityState state = ((MainActivity) context).getState();

        mOnAdoptCallback = slogan -> {
            if (state.mMyProfileManager.getProfile().getSlogans().contains(slogan)) {
                toast(R.string.ui_world_toast_slogan_already_adopted);
            } else if (state.mMyProfileManager.spaceAvailable()) {
                state.mMyProfileManager.adopt(slogan);
            } else {
                state.mDialogManager.showReplace(
                        state.mMyProfileManager.getProfile().getSlogans(),
                        sloganToReplace -> state.mMyProfileManager.replace(sloganToReplace, slogan)
                );
            }
        };
    }

    @Override
    @ExternalInvocation
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
        RecyclerView recycler = mRootView.findViewById(R.id.profile_slogans_recycler);
        recycler.setNestedScrollingEnabled(false);
        mPeerListAdapter = new PeersRecycleAdapter(getContext(), recycler, mOnAdoptCallback);
        recycler.setAdapter(mPeerListAdapter);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        // With change animations enabled items flash as updates come in
        ((SimpleItemAnimator) recycler.getItemAnimator()).setSupportsChangeAnimations(false);

        updateAllViews();
    }


    @Override
    @ExternalInvocation
    public void onPause() {
        super.onPause();
        if (mPeerListAdapter != null) {
            // onPause might be called before view has been created (activity.create -> activity.resume -> this.onResume()
            mPeerListAdapter.onPause();
        }
    }

    @Override
    @ExternalInvocation
    public void onResume() {
        super.onResume();
        if (mPeerListAdapter != null) {
            // onResume might be called before view has been created (activity.create -> activity.resume -> this.onResume()
            mPeerListAdapter.onResume();
        }
    }

    public void updateAllViews() {

        if (mRootView == null) {
            // onCreateView has not been called yet, no need to update any views.
            return;
        }

        mSwipeRefresh.setEnabled(mLastState != null && mLastState.mScanning && mLastPeers != null && mLastPeers.size() > 0);
        mSwipeRefresh.setPeerCount(mLastPeers != null ? mLastPeers.size() : 0);

        if (mLastPeers == null || mLastPeerSloganMap == null) {
            return;
        }

        reflectCommunicatorState();
    }

    @ExternalInvocation
    public void setData(@Nullable CommunicatorState state,
                        TreeMap<String, PeerSlogan> peerSloganMap,
                        Set<Peer> peers) {
        mLastState = state;
        mLastPeerSloganMap = peerSloganMap;
        mLastPeers = peers;
    }

    public void reflectCommunicatorState() {

        if (mCommunicatorStateInfoBox == null) {
            // onCreateView has not been called yet
            return;
        }

        CommunicatorStateRenderer.populateInfoBoxWithState(
                mLastState,
                mCommunicatorStateInfoBox,
                mStatusSummary,
                getContext());

//        updatePeersInfoBox();

//        updatePeersHeading();
    }

    private void updatePeersHeading() {

        View heading = mRootView.findViewById(R.id.peer_slogans_heading_wrapper);

        // Don't show heading if any info boxes are visible
        if (mCommunicatorStateInfoBox.getVisibility() == View.VISIBLE
                || mPeersInfoBox.getVisibility() == View.VISIBLE) {
            heading.setVisibility(View.GONE);
            return;

        }
        String headerText = getContext().getResources().getQuantityString(R.plurals.ui_world_peers_heading_slogans, mLastPeerSloganMap.size(), mLastPeerSloganMap.size());

        boolean synchronizing = false;
        for (Peer peer : mLastPeers) {
            if (peer.mSynchronizing) {
                synchronizing = true;
                break;
            }
        }
        mRootView.findViewById(R.id.peer_slogans_heading_progress_bar).setVisibility(
                synchronizing
                        ? View.VISIBLE
                        : View.GONE
        );

        ((TextView) mRootView.findViewById(R.id.peer_slogans_heading_text)).setText(EmojiHelper.replaceShortCode(headerText));

        heading.setVisibility(View.VISIBLE);
    }

    private void updatePeersInfoBox() {

        // Show mPeersInfoBox only if there's no communicator state info box visible and there's no peers
        if (mCommunicatorStateInfoBox.getVisibility() == View.VISIBLE || mLastPeers.size() > 0) {
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

        if (System.currentTimeMillis() - mLastState.mScanStartTimestamp < Config.MAIN_LOOKING_AROUND_SHOW_DURATION) {
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

    public void notifyPeersChanged(Set<Peer> peers) {
        if (mPeerListAdapter != null) {
            mPeerListAdapter.notifyPeersChanged(peers);
        }
    }
}
