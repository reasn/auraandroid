package io.auraapp.auraandroid.ui.world;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
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
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.common.CommunicatorStateRenderer;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.world.list.PeerAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_EXTRA_PEERS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_EXTRA_PEER;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;
import static io.auraapp.auraandroid.ui.common.CommunicatorProxy.replacePeer;

public class WorldFragment extends ScreenFragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/world/fragment";
    private InfoBox mCommunicatorStateInfoBox;
    private TextView mStatusSummary;
    private Handler mHandler = new Handler();
    private PeerAdapter mPeerAdapter;
    private FakeSwipeRefreshLayout mSwipeRefresh;

    private CommunicatorProxyState mComProxyState = null;
    private Set<Peer> mPeers = new HashSet<>();
    private RecyclerView mPeersRecycler;
    private InfoBox mPeersInfoBox;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            v(TAG, "onReceive, intent: %s", intent.getAction());
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }

            if (INTENT_PEER_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Peer peer = (Peer) extras.getSerializable(INTENT_PEER_UPDATED_EXTRA_PEER);
                if (peer != null) {
                    replacePeer(mPeers, peer, false);
                    if (mPeerAdapter != null) {
                        mPeerAdapter.notifyPeersChanged(mPeers);
                    }
                }

            } else if (INTENT_PEER_LIST_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS);
                if (peers != null) {
                    mPeers = peers;
                    if (mPeerAdapter != null) {
                        mPeerAdapter.notifyPeersChanged(mPeers);
                    }
                }
            }
            reflectState(context);
        }
    };
    private BroadcastReceiver mCommunicatorProxyStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            v(TAG, "onReceive, intent: %s", intent.getAction());
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mComProxyState = (CommunicatorProxyState) extras.getSerializable(IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE);
                reflectState(context);
            }
        }
    };

    @Override
    protected int getLayoutResource() {
        return R.layout.world_fragment;
    }

    @Override
    protected void onResumeWithContext(MainActivity activity, ViewGroup rootView) {

        activity.registerReceiver(mReceiver, IntentFactory.communicatorIntentFilter());
        LocalBroadcastManager.getInstance(activity).registerReceiver(mCommunicatorProxyStateReceiver, IntentFactory.createFilter(LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION));
        v(TAG, "Receivers registered");

        SharedServicesSet servicesSet = activity.getSharedServicesSet();
        mPeers = servicesSet.mCommunicatorProxy.getPeers();
        mComProxyState = servicesSet.mCommunicatorProxy.getState();

        mStatusSummary = rootView.findViewById(R.id.profile_status_summary);
        mCommunicatorStateInfoBox = rootView.findViewById(R.id.profile_status_info_box);
        mPeersInfoBox = rootView.findViewById(R.id.peer_slogans_info_box);
        mSwipeRefresh = rootView.findViewById(R.id.fake_swipe_to_refresh);
        mSwipeRefresh.setEnabled(false);
        mPeersRecycler = rootView.findViewById(R.id.profile_slogans_recycler);

        mPeerAdapter = new PeerAdapter(activity, mPeersRecycler, slogan -> {
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
        });
        mPeersRecycler.setAdapter(mPeerAdapter);
        mPeersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mPeersRecycler.setNestedScrollingEnabled(false);
        // With change animations enabled items flash as updates come in
        ((SimpleItemAnimator) mPeersRecycler.getItemAnimator()).setSupportsChangeAnimations(false);
        mPeerAdapter.onResume();

        reflectState(activity);
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        activity.unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mCommunicatorProxyStateReceiver);
        v(TAG, "Receivers unregistered");
        mPeerAdapter.onPause();
    }

    public void reflectState(Context context) {

        boolean scanning = mComProxyState.mEnabled
                && mComProxyState.mCommunicatorState != null
                && mComProxyState.mCommunicatorState.mScanning;

        mSwipeRefresh.setEnabled(scanning
                && mPeers != null
                && mPeers.size() > 0);
        mSwipeRefresh.setPeerCount(mPeers != null ? mPeers.size() : 0);

        CommunicatorStateRenderer.populateInfoBoxWithState(
                mComProxyState,
                mCommunicatorStateInfoBox,
                mStatusSummary,
                getContext());
        mPeersRecycler.setVisibility(scanning
                ? View.VISIBLE
                : View.GONE
        );
//        updatePeersHeading();

        CommunicatorState communicatorState = mComProxyState.mCommunicatorState;
        if (communicatorState == null || mPeers.size() > 0) {
            // Show mPeersInfoBox only if there's no communicator state info box visible and there's no peers
            mPeersInfoBox.setVisibility(View.GONE);
        } else {
            updatePeersInfoBox(context, communicatorState);
        }
    }

    private void updatePeersInfoBox(Context context, CommunicatorState communicatorState) {

        long scanDuration = System.currentTimeMillis() - communicatorState.mScanStartTimestamp;
        if (scanDuration < Config.MAIN_LOOKING_AROUND_SHOW_DURATION) {
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
                                EmojiHelper.replaceShortCode(context.getString(R.string.ui_main_share_text))
                        );
                        sendIntent.setType("text/plain");
                        context.startActivity(sendIntent);
                    });
        }

        mPeersInfoBox.setBackgroundColor(context.getResources().getColor(R.color.infoBoxWarning));
        mPeersInfoBox.setVisibility(View.VISIBLE);
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
}
