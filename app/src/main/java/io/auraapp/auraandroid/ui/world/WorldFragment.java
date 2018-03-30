package io.auraapp.auraandroid.ui.world;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.world.list.OnAdoptCallback;
import io.auraapp.auraandroid.ui.world.list.PeerSlogansRecycleAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.w;

public class WorldFragment extends Fragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/world/fragment";
    private ViewGroup mRootView;
    private TextView mPeerSlogansHeadingText;
    private ProgressBar mPeerSlogansHeadingProgressBar;
    private InfoBox mPeerSlogansHeadingInfoBox;
    private Context mContext;
    private InfoBox mStatusInfoBox;
    private TextView mStatusSummary;
    private boolean mHasView = false;
    private Runnable mDeferredUpdate = null;
    private Handler mHandler = new Handler();
    private OnAdoptCallback mOnAdoptCallback;
    private RecyclerView mPeerListView;
    private PeerSlogansRecycleAdapter mPeerListAdapter;
    private FakeSwipeRefreshLayout mSwipeRefresh;

    public static WorldFragment create(Context context, OnAdoptCallback onAdoptCallback) {
        WorldFragment fragment = new WorldFragment();
        fragment.mContext = context;
        fragment.mOnAdoptCallback = onAdoptCallback;
        return fragment;
    }

    @Override
    @ExternalInvocation
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(R.layout.world_fragment, container, false);

        mHandler.post(() -> {

            mStatusSummary = mRootView.findViewById(R.id.status_summary);
            mStatusInfoBox = mRootView.findViewById(R.id.status_info_box);

            mPeerSlogansHeadingText = mRootView.findViewById(R.id.peer_slogans_heading_text);
            mPeerSlogansHeadingProgressBar = mRootView.findViewById(R.id.peer_slogans_heading_progress_bar);
            mPeerSlogansHeadingInfoBox = mRootView.findViewById(R.id.peer_slogans_info_box);

            mPeerListView = mRootView.findViewById(R.id.list_view);
            mPeerListView.setNestedScrollingEnabled(false);

            mPeerListAdapter = new PeerSlogansRecycleAdapter(mContext, mPeerListView, mOnAdoptCallback);

            mPeerListView.setAdapter(mPeerListAdapter);
            mPeerListView.setLayoutManager(new LinearLayoutManager(mContext));

            // With change animations enabled mStatusItem keeps flashing because updates come in
            ((SimpleItemAnimator) mPeerListView.getItemAnimator()).setSupportsChangeAnimations(false);

            mSwipeRefresh = mRootView.findViewById(R.id.fake_swipe_to_refresh);
            mSwipeRefresh.setEnabled(false);

            mHasView = true;
            if (mDeferredUpdate != null) {
                mDeferredUpdate.run();
                mDeferredUpdate = null;
            }
        });

        return mRootView;
    }

    @Override
    @ExternalInvocation
    public void onPause() {
        super.onPause();
        mHandler.post(() -> {
            mPeerListAdapter.onPause();
        });
    }

    @Override
    @ExternalInvocation
    public void onResume() {
        super.onResume();
        if (mHasView) {
            // Checking for mHasView because activity.create>activity.resume calls this.onResume()
            // before this.onCreateView has been called (is called after activity.onResume()
            mHandler.post(() -> {
                mPeerListAdapter.onResume();
            });
        }
    }

    public void update(@Nullable CommunicatorState state,
                       TreeMap<String, PeerSlogan> peerSloganMap,
                       Set<Peer> peers) {
        mHandler.post(() -> {

            Runnable r = () -> {

                updateStatus(state, peerSloganMap, peers);
                bindPeerSlogansHeading(peers, peerSloganMap.size());
                if (state != null) {
                    updatePeersInfoBox(peers, state.mScanning, state.mScanStartTimestamp);
                    mSwipeRefresh.setEnabled(state.mScanning);
                    mSwipeRefresh.setPeerCount(peers.size());
                }
            };
            if (mHasView) {
                r.run();
            } else {
                mDeferredUpdate = r;
            }
        });
    }

    private void bindPeerSlogansHeading(Set<Peer> peers, int peerSloganCount) {
        String heading;
        if (peers.size() == 0) {
            heading = mContext.getString(R.string.ui_main_peers_heading_no_peers);
        } else {
            heading = mContext.getResources().getQuantityString(R.plurals.ui_main_peers_heading_slogans, peerSloganCount, peerSloganCount);
        }

        boolean synchronizing = false;
        for (Peer peer : peers) {
            if (peer.mSynchronizing) {
                synchronizing = true;
                break;
            }
        }
        mPeerSlogansHeadingProgressBar.setVisibility(
                synchronizing
                        ? View.VISIBLE
                        : View.GONE
        );
        mPeerSlogansHeadingText.setText(EmojiHelper.replaceShortCode(heading));

    }

    private void updatePeersInfoBox(Set<Peer> peers, boolean scanning, long scanStartTimestamp) {
//        int nearbyPeers = 0;
//        long now = System.currentTimeMillis();
//        for (Peer peer : peers) {
//            if (now - peer.mLastSeenTimestamp < 30000) {
//                nearbyPeers++;
//            }
//        }


        if (peers.size() == 0) {
            mPeerSlogansHeadingInfoBox.setEmoji(":see_no_evil:");
            if (scanning) {

                if (System.currentTimeMillis() - scanStartTimestamp < Config.MAIN_LOOKING_AROUND_SHOW_DURATION) {
                    mPeerSlogansHeadingInfoBox.setHeading(R.string.ui_main_status_peers_starting_heading);
                    mPeerSlogansHeadingInfoBox.setText(R.string.ui_main_status_peers_starting_text);
                    mPeerSlogansHeadingInfoBox.setEmoji(":satellite_antenna:");
                    mPeerSlogansHeadingInfoBox.hideButton();

                } else {
                    mPeerSlogansHeadingInfoBox.setHeading(R.string.ui_main_status_peers_no_peers_info_heading);
                    mPeerSlogansHeadingInfoBox.setText(R.string.ui_main_status_peers_no_peers_info_text);
                    mPeerSlogansHeadingInfoBox.showButton(
                            R.string.ui_main_status_peers_no_peers_info_heading_cta,
                            R.string.ui_main_status_peers_no_peers_info_second_text,
                            $ -> {
                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(
                                        Intent.EXTRA_TEXT,
                                        EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_share_text))
                                );
                                sendIntent.setType("text/plain");
                                mContext.startActivity(sendIntent);
                            });
                }
            } else {
                mPeerSlogansHeadingInfoBox.setHeading(R.string.ui_main_status_peers_not_scanning_heading);
                mPeerSlogansHeadingInfoBox.setText(R.string.ui_main_status_peers_not_scanning_text);
                mPeerSlogansHeadingInfoBox.hideButton();
            }
            mPeerSlogansHeadingInfoBox.setBackgroundColor(mContext.getResources().getColor(R.color.infoBoxWarning));
            mPeerSlogansHeadingInfoBox.setVisibility(View.VISIBLE);
//        } else if (sloganCount == 0) {
//            mPeerSlogansHeadingInfoBox.setEmoji(":silhouette:");
//            mPeerSlogansHeadingInfoBox.setHeading(R.string.ui_main_peers_heading_no_slogans_heading);
//            mPeerSlogansHeadingInfoBox.setText(R.string.ui_main_peers_heading_no_slogans_text);
//            mPeerSlogansHeadingInfoBox.hideButton();
//            mPeerSlogansHeadingInfoBox.setColor(R.color.infoBoxNeutral);
//            mPeerSlogansHeadingInfoBox.setVisibility(View.VISIBLE);
//            return;
        } else {
            mPeerSlogansHeadingInfoBox.setVisibility(View.GONE);
        }
    }

    private void showAuraOffInfoBox() {
        mStatusInfoBox.setEmoji(":sleeping_sign:");
        mStatusInfoBox.setHeading(R.string.ui_main_status_communicator_disabled_heading);
        mStatusInfoBox.setText(R.string.ui_main_status_communicator_disabled_text);
        mStatusInfoBox.hideButton();
        mStatusInfoBox.setColor(R.color.infoBoxWarning);
    }

    private void updateStatus(@Nullable CommunicatorState state,
                              TreeMap<String, PeerSlogan> peerSloganMap,
                              Set<Peer> peers) {

        if (peerSloganMap == null || peers == null) {
            return;
        }

        final int NONE = 0;
        final int BOX = 1;
        final int MESSAGE = 2;

        // The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
        int show = MESSAGE;
        if (state == null) {
            showAuraOffInfoBox();
            show = BOX;

        } else if (state.mBluetoothRestartRequired) {
            mStatusInfoBox.setEmoji(":dizzy_face:");
            mStatusInfoBox.setHeading(R.string.ui_main_status_communicator_bt_restart_required_heading);
            mStatusInfoBox.setText(mContext.getString(R.string.ui_main_status_communicator_bt_restart_required_text)
                    .replaceAll("##error##", state.mLastError != null ? state.mLastError : "unknown"));
            mStatusInfoBox.hideButton();
            mStatusInfoBox.setColor(R.color.infoBoxError);
            show = BOX;

        } else if (state.mBtTurningOn) {
            mStatusSummary.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_status_summary_communicator_bt_turning_on)));
            mStatusSummary.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
        } else if (!state.mBtEnabled) {
            mStatusInfoBox.setEmoji(":broken_heart:");
            mStatusInfoBox.setHeading(R.string.ui_main_status_communicator_bt_disabled_heading);
            mStatusInfoBox.setText(R.string.ui_main_status_communicator_bt_disabled_text);
            mStatusInfoBox.hideButton();
            mStatusInfoBox.setColor(R.color.infoBoxWarning);
            show = BOX;
        } else if (!state.mBleSupported) {
            mStatusInfoBox.setEmoji(":dizzy_face:");
            mStatusInfoBox.setHeading(mContext.getString(R.string.ui_main_status_communicator_ble_not_supported_heading));
            mStatusInfoBox.setText(R.string.ui_main_status_communicator_ble_not_supported_text);
            mStatusInfoBox.hideButton();
            mStatusInfoBox.setColor(R.color.infoBoxError);
            show = BOX;
        } else if (!state.mShouldCommunicate) {
            showAuraOffInfoBox();
            show = BOX;
        } else if (!state.mAdvertisingSupported) {
            mStatusInfoBox.setEmoji(":broken_heart:");
            mStatusInfoBox.setHeading(R.string.ui_main_status_communicator_advertising_not_supported_heading);
            mStatusInfoBox.setText(R.string.ui_main_status_communicator_advertising_not_supported_text);
            mStatusInfoBox.hideButton();
            mStatusInfoBox.setColor(R.color.infoBoxWarning);
            show = BOX;
        } else if (!state.mAdvertising) {
            w(TAG, "Not advertising although it is possible.");
            mStatusSummary.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_status_summary_communicator_on_not_active)));
            mStatusSummary.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
        } else if (!state.mScanning) {
            w(TAG, "Not scanning although it is possible.");
            mStatusSummary.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_status_summary_communicator_on_not_active)));
            mStatusSummary.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
        } else {
            show = NONE;
        }
        // The following LayoutParams magic is necessary to hide the list item entirely if show == BOX
        // because setting the item's visibility to GONE doesn't do the job.
        // Thanks to https://stackoverflow.com/questions/41223413/how-to-hide-an-item-from-recycler-view-on-a-particular-condition
        if (show == BOX) {
            mStatusSummary.setVisibility(View.GONE);
            mStatusInfoBox.setVisibility(View.VISIBLE);
        } else if (show == MESSAGE) {
            mStatusSummary.setVisibility(View.VISIBLE);
            mStatusSummary.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mStatusInfoBox.setVisibility(View.GONE);
        } else {
            mStatusInfoBox.setVisibility(View.GONE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.height = 0;
            mStatusSummary.setLayoutParams(params);
        }
    }

    public void notifyPeerSlogansChanged(TreeMap<String, PeerSlogan> mPeerSloganMap) {
        if (mPeerListAdapter != null) {
            mPeerListAdapter.notifyPeerSloganListChanged(mPeerSloganMap);
        }
    }
}
