package io.auraapp.auraandroid.ui.world;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.common.ProductionStubFactory;
import io.auraapp.auraandroid.ui.common.fragments.ContextViewFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfile;
import io.auraapp.auraandroid.ui.world.list.PeerAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_EXTRA_PEERS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_EXTRA_PEER;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_EXTRA_PROFILE;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_COMPLETE_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_OPEN_ACTION;
import static io.auraapp.auraandroid.ui.common.CommunicatorProxy.replacePeer;

public class WorldFragment extends ContextViewFragment {

    private static final String TAG = "@aura/ui/world/fragment";
    private Handler mHandler = new Handler();
    private PeerAdapter mPeerAdapter;
    private FakeSwipeRefreshLayout mSwipeRefresh;

    private CommunicatorProxyState mComProxyState = null;
    private Set<Peer> mPeers = new HashSet<>();
    private RecyclerView mPeersRecycler;
    private LinearLayout mStartingWrapper;
    private View mNoPeersWrapper;
    private Button mInviteButton;
    private TextView mNotScanningMessage;
    private String mMyColor;
    private Set<Peer> mTutorialPeers;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            v(TAG, "onReceive, intent: %s", intent.getAction());
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }

            if (mTutorialPeers != null) {
                return;
            }
            if (INTENT_PEER_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Peer peer = (Peer) extras.getSerializable(INTENT_PEER_UPDATED_EXTRA_PEER);
                if (peer != null) {
                    replacePeer(mPeers, peer, false);
                    if (mPeerAdapter != null) {
                        mPeerAdapter.notifyPeerChanged(peer);
                    }
                }

            } else if (INTENT_PEER_LIST_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS);
                if (peers != null) {
                    mPeers = peers;
                    if (mPeerAdapter != null) {
                        mPeerAdapter.notifyPeerListChanged(mPeers);
                    }
                }
            }
            reflectState(context);
        }
    };
    private BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            v(TAG, "onReceive, intent: %s", intent.getAction());

            if (LOCAL_TUTORIAL_OPEN_ACTION.equals(intent.getAction())) {
                mTutorialPeers = ProductionStubFactory.createFakePeers();
                if (mPeerAdapter != null) {
                    mPeerAdapter.notifyPeerListChanged(mTutorialPeers);
                    reflectState(context);
                }
                return;
            }
            if (LOCAL_TUTORIAL_COMPLETE_ACTION.equals(intent.getAction())) {
                mTutorialPeers = null;
                if (mPeerAdapter != null) {
                    mPeerAdapter.notifyPeerListChanged(mPeers);
                    reflectState(context);
                }
                return;
            }

            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }
            if (LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                mComProxyState = (CommunicatorProxyState) extras.getSerializable(LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE);
                reflectState(context);
            } else if (LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION.equals(intent.getAction())) {
                mMyColor = ((MyProfile) extras.getSerializable(LOCAL_MY_PROFILE_EXTRA_PROFILE)).getColor();
            }
        }
    };

    @Override
    protected int getLayoutResource() {
        return R.layout.world_fragment;
    }

    @Override
    protected void onResumeWithContextAndView(MainActivity activity, ViewGroup rootView) {

        activity.registerReceiver(mReceiver, IntentFactory.communicatorIntentFilter());
        LocalBroadcastManager.getInstance(activity).registerReceiver(mLocalReceiver,
                IntentFactory.createFilter(
                        LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION,
                        LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION,
                        LOCAL_TUTORIAL_OPEN_ACTION,
                        LOCAL_TUTORIAL_COMPLETE_ACTION
                ));
        v(TAG, "Receivers registered");

        SharedServicesSet servicesSet = activity.getSharedServicesSet();
        mPeers = servicesSet.mCommunicatorProxy.getPeers();
        mComProxyState = servicesSet.mCommunicatorProxy.getState();
        mMyColor = servicesSet.mMyProfileManager.getColor();
        v(TAG, "%s fake peers for tutorial", servicesSet.mTutorialManager.isOpen() ? "Showing" : "Not showing");
        if (servicesSet.mTutorialManager.isOpen()) {
            mTutorialPeers = ProductionStubFactory.createFakePeers();
        }

        mNotScanningMessage = rootView.findViewById(R.id.world_not_scanning);
        mStartingWrapper = rootView.findViewById(R.id.world_starting_wrapper);
        mNoPeersWrapper = rootView.findViewById(R.id.world_no_peers_wrapper);
        mInviteButton = rootView.findViewById(R.id.world_invite);
        mSwipeRefresh = rootView.findViewById(R.id.fake_swipe_to_refresh);
        mSwipeRefresh.setEnabled(false);
        mPeersRecycler = rootView.findViewById(R.id.profile_slogans_recycler);

        mPeerAdapter = new PeerAdapter(activity, mPeersRecycler,
                () -> Color.parseColor(mMyColor),
                slogan -> {
                    if (servicesSet.mMyProfileManager.getProfile().getSlogans().contains(slogan)) {
                        toast(R.string.world_toast_slogan_already_adopted);
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
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mLocalReceiver);
        v(TAG, "Receivers unregistered");
        mPeerAdapter.onPause();
    }

    public void reflectState(Context context) {

        boolean scanning = mComProxyState.mEnabled
                && mComProxyState.mCommunicatorState != null
                && mComProxyState.mCommunicatorState.mScanning;

        int peersCount = mTutorialPeers != null
                ? mTutorialPeers.size()
                : mPeers != null ? mPeers.size() : 0;

        mPeersRecycler.setVisibility(scanning ? View.VISIBLE : View.GONE);
        mNotScanningMessage.setVisibility(scanning ? View.GONE : View.VISIBLE);
        mSwipeRefresh.setEnabled(scanning);
        mSwipeRefresh.setPeerCount(peersCount);

        if (!scanning || peersCount > 0) {
            mStartingWrapper.setVisibility(View.GONE);
            mNoPeersWrapper.setVisibility(View.GONE);
            return;
        }

        CommunicatorState communicatorState = mComProxyState.mCommunicatorState;
        long scanDuration = System.currentTimeMillis() - communicatorState.mScanStartTimestamp;

        if (scanDuration < Config.MAIN_LOOKING_AROUND_SHOW_DURATION) {
            // Scan just started, let's make sure we hide the "looking around" info if
            // nothing is found for some time.
            mHandler.postDelayed(() -> reflectState(context), Config.MAIN_LOOKING_AROUND_SHOW_DURATION);
            mStartingWrapper.setVisibility(View.VISIBLE);
            mNoPeersWrapper.setVisibility(View.GONE);
            return;
        }

        mStartingWrapper.setVisibility(View.GONE);
        mNoPeersWrapper.setVisibility(View.VISIBLE);
        mInviteButton.setOnClickListener($ -> {
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
}
