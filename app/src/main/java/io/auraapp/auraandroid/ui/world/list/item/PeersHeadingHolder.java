package io.auraapp.auraandroid.ui.world.list.item;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Set;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.common.InfoBox;

public class PeersHeadingHolder extends ItemViewHolder {

    private final TextView mHeadingTextView;
    private final ProgressBar mProgressBar;
    private Context mContext;
    private final InfoBox mInfoBox;

    public PeersHeadingHolder(View itemView, Context context) {
        super(itemView);
        mContext = context;
        mHeadingTextView = itemView.findViewById(R.id.heading);
        mProgressBar = itemView.findViewById(R.id.progressBar);
        mInfoBox = itemView.findViewById(R.id.info_box);
    }

    @Override
    public void bind(ListItem item, View mItemView) {
        if (!(item instanceof PeersHeadingItem)) {
            throw new RuntimeException("Expecting " + PeersHeadingItem.class.getSimpleName());
        }
        PeersHeadingItem castItem = ((PeersHeadingItem) item);
        String heading;
        if (castItem.mPeers.size() == 0) {
            heading = mContext.getString(R.string.ui_main_peers_heading_no_peers);
        } else {
            heading = mContext.getResources().getQuantityString(R.plurals.ui_main_peers_heading_slogans, castItem.mSloganCount, castItem.mSloganCount);
        }

        boolean synchronizing = false;
        for (Peer peer : castItem.mPeers) {
            if (peer.mSynchronizing) {
                synchronizing = true;
                break;
            }
        }
        mProgressBar.setVisibility(
                synchronizing
                        ? View.VISIBLE
                        : View.GONE
        );
        mHeadingTextView.setText(EmojiHelper.replaceShortCode(heading));

        updateInfoBox(castItem.mPeers, castItem.mSloganCount, castItem.mScanning, castItem.mScanStartTimestamp);
    }

    private void updateInfoBox(Set<Peer> peers, int sloganCount, boolean scanning, long scanStartTimestamp) {
//        int nearbyPeers = 0;
//        long now = System.currentTimeMillis();
//        for (Peer peer : peers) {
//            if (now - peer.mLastSeenTimestamp < 30000) {
//                nearbyPeers++;
//            }
//        }


        if (peers.size() == 0) {
            mInfoBox.setEmoji(":see_no_evil:");
            if (scanning) {

                if (System.currentTimeMillis() - scanStartTimestamp < Config.MAIN_LOOKING_AROUND_SHOW_DURATION) {
                    mInfoBox.setHeading(R.string.ui_main_status_peers_starting_heading);
                    mInfoBox.setText(R.string.ui_main_status_peers_starting_text);
                    mInfoBox.setEmoji(":satellite_antenna:");
                    mInfoBox.hideButton();

                } else {
                    mInfoBox.setHeading(R.string.ui_main_status_peers_no_peers_info_heading);
                    mInfoBox.setText(R.string.ui_main_status_peers_no_peers_info_text);
                    mInfoBox.showButton(
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
                mInfoBox.setHeading(R.string.ui_main_status_peers_not_scanning_heading);
                mInfoBox.setText(R.string.ui_main_status_peers_not_scanning_text);
                mInfoBox.hideButton();
            }
            mInfoBox.setBackgroundColor(mContext.getResources().getColor(R.color.infoBoxWarning));
            mInfoBox.setVisibility(View.VISIBLE);
//        } else if (sloganCount == 0) {
//            mInfoBox.setEmoji(":silhouette:");
//            mInfoBox.setHeading(R.string.ui_main_peers_heading_no_slogans_heading);
//            mInfoBox.setText(R.string.ui_main_peers_heading_no_slogans_text);
//            mInfoBox.hideButton();
//            mInfoBox.setColor(R.color.infoBoxNeutral);
//            mInfoBox.setVisibility(View.VISIBLE);
//            return;
        } else {
            mInfoBox.setVisibility(View.GONE);
        }

    }
}
