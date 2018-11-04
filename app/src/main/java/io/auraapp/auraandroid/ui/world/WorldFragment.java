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

import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.common.fragments.ContextViewFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfile;
import io.auraapp.auraandroid.ui.world.list.PeerAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.quickDump;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_EXTRA_PEERS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_EXTRA_PEER;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_EXTRA_PROFILE;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_COMPLETED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_TUTORIAL_OPENED_ACTION;

public class WorldFragment extends ContextViewFragment {

    private static final String TAG = "@aura/ui/world/fragment";
    private final Handler mHandler = new Handler();
    private PeerAdapter mPeerAdapter;
    private FakeSwipeRefreshLayout mSwipeRefresh;

    private CommunicatorProxyState mComProxyState = null;
    private RecyclerView mPeersRecycler;
    private LinearLayout mStartingWrapper;
    private View mNoPeersWrapper;
    private Button mInviteButton;
    private TextView mNotScanningMessage;
    private String mMyColor;
    private boolean mTutorialOpen;
    private Runnable mUnregisterPrefListener;

    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            v(TAG, "onReceive, local intent: %s", intent.getAction());

            if (LOCAL_TUTORIAL_OPENED_ACTION.equals(intent.getAction())) {
                mTutorialOpen = true;
                reflectState(context);
                return;
            }
            if (LOCAL_TUTORIAL_COMPLETED_ACTION.equals(intent.getAction())) {
                mTutorialOpen = false;
                reflectState(context);
                return;
            }

            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }

            if (INTENT_PEER_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Peer peer = (Peer) extras.getSerializable(INTENT_PEER_UPDATED_EXTRA_PEER);
                if (peer != null && mPeerAdapter != null) {
                    mPeerAdapter.notifyPeerChanged(peer);
                }
                reflectState(context);
                return;

            } else if (INTENT_PEER_LIST_UPDATED_ACTION.equals(intent.getAction())) {
                @SuppressWarnings("unchecked")
                Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS);
                if (peers != null && mPeerAdapter != null) {
                    mPeerAdapter.notifyPeerListChanged(peers);
                }
                reflectState(context);
                return;
            }
            if (LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                mComProxyState = (CommunicatorProxyState) extras.getSerializable(LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE);
                reflectState(context);
                return;
            }
            if (LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION.equals(intent.getAction())) {
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
        d(TAG, "onResumeWithContextAndView");

        mUnregisterPrefListener = AuraPrefs.listen(activity, R.string.prefs_debug_fake_peers_key, value -> reflectState(getActivity()));
        LocalBroadcastManager.getInstance(activity).registerReceiver(mLocalReceiver,
                IntentFactory.createFilter(
                        LOCAL_TUTORIAL_OPENED_ACTION,
                        LOCAL_TUTORIAL_COMPLETED_ACTION,
                        LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION,
                        LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION,
                        INTENT_PEER_LIST_UPDATED_ACTION,
                        INTENT_PEER_UPDATED_ACTION
                ));

        SharedServicesSet servicesSet = activity.getSharedServicesSet();
        Set<Peer> peers = servicesSet.mCommunicatorProxy.getPeers();
        mComProxyState = servicesSet.mCommunicatorProxy.getState();
        mMyColor = servicesSet.mMyProfileManager.getColor();

        mTutorialOpen = servicesSet.mTutorialManager.isOpen();

        v(TAG, "Receivers registered, peers fetched, peers: %d, mComProxyState: %s", peers.size(), mComProxyState);

        mNotScanningMessage = rootView.findViewById(R.id.world_not_scanning);
        mStartingWrapper = rootView.findViewById(R.id.world_starting_wrapper);
        mNoPeersWrapper = rootView.findViewById(R.id.world_no_peers_wrapper);
        mInviteButton = rootView.findViewById(R.id.world_invite);
        mSwipeRefresh = rootView.findViewById(R.id.fake_swipe_to_refresh);
        mSwipeRefresh.setEnabled(false);
        mPeersRecycler = rootView.findViewById(R.id.world_peers_recycler);

        mPeerAdapter = new PeerAdapter(activity,
                servicesSet.mTutorialManager.isOpen(),
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

        v(TAG, "%s fake peers for tutorial", servicesSet.mTutorialManager.isOpen() ? "Showing" : "Not showing");
        mPeerAdapter.notifyPeerListChanged(peers);

        mPeersRecycler.setAdapter(mPeerAdapter);
        mPeersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mPeersRecycler.setNestedScrollingEnabled(false);
        // With change animations enabled items flash as updates come in
        ((SimpleItemAnimator) mPeersRecycler.getItemAnimator()).setSupportsChangeAnimations(false);
        mPeerAdapter.onResume();

        reflectState(activity);

        v(TAG, "Updated view, peers: %d", peers.size());
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        d(TAG, "onPauseWithContext");
        if (mUnregisterPrefListener != null) {
            mUnregisterPrefListener.run();
        }
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mLocalReceiver);
        mPeerAdapter.onPause();
        v(TAG, "Receivers unregistered, adapter paused");
    }

    private void reflectState(Context context) {

        boolean scanning = mComProxyState.mEnabled
                && mComProxyState.mCommunicatorState != null
                && mComProxyState.mCommunicatorState.mScanning;


        boolean fakePeersEnabled = mTutorialOpen || AuraPrefs.areDebugFakePeersEnabled(context);

        mPeerAdapter.toggleFakePeers(mTutorialOpen);

        int peersCount = mPeerAdapter.getVisiblePeers().size();

        mPeersRecycler.setVisibility(fakePeersEnabled || scanning ? View.VISIBLE : View.GONE);
        mNotScanningMessage.setVisibility(fakePeersEnabled || scanning ? View.GONE : View.VISIBLE);
        mSwipeRefresh.setEnabled(scanning);
        mSwipeRefresh.setPeerCount(peersCount);

        v(TAG, "Reflecting state, scanning: %b, peersCount: %d", scanning, peersCount);

        quickDump("----");
        quickDump(mTutorialOpen);
        quickDump(fakePeersEnabled);
        quickDump(scanning);
        quickDump(peersCount);

        if (fakePeersEnabled || !scanning || peersCount > 0) {
            mStartingWrapper.setVisibility(View.GONE);
            mNoPeersWrapper.setVisibility(View.GONE);
            return;
        }

        mStartingWrapper.setVisibility(View.VISIBLE);
        mNoPeersWrapper.setVisibility(View.VISIBLE);

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
