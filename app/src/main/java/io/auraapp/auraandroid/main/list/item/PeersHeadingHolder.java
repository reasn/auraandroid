package io.auraapp.auraandroid.main.list.item;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;

public class PeersHeadingHolder extends ItemViewHolder {

    private final TextView mHeadingTextView;
    private final ProgressBar mProgressBar;
    private Context mContext;
    private TextView mInfoTextView;

    public PeersHeadingHolder(View itemView, Context context) {
        super(itemView);
        mContext = context;
        mHeadingTextView = itemView.findViewById(R.id.heading);
        mProgressBar = itemView.findViewById(R.id.progressBar);
        mInfoTextView = itemView.findViewById(R.id.info);
    }

    @Override
    public void bind(ListItem item, View mItemView) {
        if (!(item instanceof PeersHeadingItem)) {
            throw new RuntimeException("Expecting " + PeersHeadingItem.class.getSimpleName());
        }
        PeersHeadingItem castItem = ((PeersHeadingItem) item);
        String heading;
        String info = null;
        if (castItem.mPeers.size() == 0) {
            heading = mContext.getString(R.string.ui_main_peers_heading_no_peers);
            info = mContext.getString(R.string.ui_main_peers_heading_no_peers_text);
        } else {
            heading = mContext.getResources().getQuantityString(R.plurals.ui_main_peers_heading_slogans, castItem.mSloganCount, castItem.mSloganCount);
            if (castItem.mSloganCount == 0) {
                info = mContext.getString(R.string.ui_main_peers_heading_no_slogans_text);
            }
        }

        long now = System.currentTimeMillis();
        boolean fetching = false;
        for (Peer peer : castItem.mPeers) {
            if (peer.mNextFetch <= now) {
                fetching = true;
                break;
            }
        }
        mProgressBar.setVisibility(
                fetching
                        ? View.VISIBLE
                        : View.GONE
        );

        mHeadingTextView.setText(EmojiHelper.replaceShortCode(heading));
        if (info != null) {
            mInfoTextView.setText(EmojiHelper.replaceShortCode(info));
            mInfoTextView.setVisibility(View.VISIBLE);
        } else {
            mInfoTextView.setVisibility(View.GONE);
        }

    }
}
