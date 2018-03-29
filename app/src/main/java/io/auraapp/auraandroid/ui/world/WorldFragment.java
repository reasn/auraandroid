package io.auraapp.auraandroid.ui.world;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Set;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.common.InfoBox;

public class WorldFragment extends Fragment implements FragmentWithToolbarButtons {

    private ViewGroup mRootView;
    private TextView mPeerSlogansHeadingText;
    private ProgressBar mPeerSlogansHeadingProgressBar;
    private InfoBox mPeerSlogansHeadingInfoBox;
    private Context mContext;

    public static WorldFragment create(Context context, ViewGroup worldView) {
        WorldFragment fragment = new WorldFragment();
        fragment.mContext = context;
        fragment.mRootView = worldView;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mPeerSlogansHeadingText = mRootView.findViewById(R.id.peer_slogans_heading_text);
        mPeerSlogansHeadingProgressBar = mRootView.findViewById(R.id.peer_slogans_heading_progress_bar);
        mPeerSlogansHeadingInfoBox = mRootView.findViewById(R.id.peer_slogans_info_box);

        return mRootView;
    }


    public void update(Set<Peer> peers, int peerSloganCount, boolean scanning, long scanStartTimestamp) {
        bindPeerSlogansHeading(peers, peerSloganCount);
        updateInfoBox(peers, scanning, scanStartTimestamp);
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

    private void updateInfoBox(Set<Peer> peers, boolean scanning, long scanStartTimestamp) {
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
}
