package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;

import static io.auraapp.auraandroid.common.FormattedLog.e;

public class PeerSloganItemHolder extends ItemViewHolder {

    private static final String TAG = "aura/list/" + PeerSloganItemHolder.class.getSimpleName();
    private final TextView mSloganTextView;
    private final TextView mLastSeenTextView;
    private final Context mContext;
    private final LinearLayout mExpandedWrapper;
    private final Button mPeerInfoButton;
    private final Button mAdoptButton;
    private final OnAdoptCallback mOnAdoptCallback;

    public PeerSloganItemHolder(View itemView,
                                Context context,
                                PeerSlogansRecycleAdapter.CollapseExpandHandler collapseExpandHandler,
                                OnAdoptCallback onAdoptCallback) {
        super(itemView);

        mContext = context;
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        mLastSeenTextView = itemView.findViewById(R.id.last_seen);
        mPeerInfoButton = itemView.findViewById(R.id.peer_info_button);
        mAdoptButton = itemView.findViewById(R.id.adopt_button);
        mExpandedWrapper = itemView.findViewById(R.id.expanded_wrapper);
        mOnAdoptCallback = onAdoptCallback;

        itemView.setOnClickListener($ -> collapseExpandHandler.flip(getLastBoundItem()));
    }

    @Override
    public void bind(ListItem item, View itemView) {

        if (item == null) {
            e(TAG, "Trying to bind %s to null ListItem", PeerSloganItemHolder.class.getSimpleName());
            return;
        }
        if (!(item instanceof PeerSloganListItem)) {
            e(TAG, "Trying to bind %s with %s", PeerSloganItemHolder.class.getSimpleName(), item.getClass().getSimpleName());
            return;
        }

        PeerSloganListItem castItem = (PeerSloganListItem) item;

        mSloganTextView.setText(castItem.getSlogan().getText());

        mAdoptButton.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_world_peer_slogan_adopt)));
        mAdoptButton.setOnClickListener($ -> mOnAdoptCallback.onAdoptIntended(castItem.getSlogan()));

        mPeerInfoButton.setText(EmojiHelper.replaceShortCode(mContext.getResources().getQuantityString(
                R.plurals.ui_world_peer_slogan_authors, castItem.getPeers().size(), castItem.getPeers().size()
        )));

        if (!castItem.mExpanded) {
            mExpandedWrapper.setVisibility(View.GONE);
            return;
        }
        // TODO animate

        mExpandedWrapper.setVisibility(View.VISIBLE);

        long lastSeen = castItem.getLastSeen();
        long elapsedSeconds = Math.round((System.currentTimeMillis() - lastSeen) / 1000);

        String text;
        if (elapsedSeconds < 10) {
            text = mContext.getString(R.string.ui_main_peer_slogan_last_seen_lt_10s);
        } else if (elapsedSeconds < 60) {
            text = mContext.getString(R.string.ui_main_peer_slogan_last_seen_lt_1min);
        } else if (elapsedSeconds < 10 * 60) {
            text = mContext.getString(R.string.ui_main_peer_slogan_last_seen_lt_10min)
                    .replace("##elapsed_minutes##", (int) Math.ceil(elapsedSeconds / 60) + "");
        } else if (elapsedSeconds < 30 * 60) {
            text = mContext.getString(R.string.ui_main_peer_slogan_last_seen_lt_30min);
        } else if (elapsedSeconds < 45 * 60) {
            text = mContext.getString(R.string.ui_main_peer_slogan_last_seen_lt_45min);
        } else if (elapsedSeconds < 61 * 60) {
            text = mContext.getString(R.string.ui_main_peer_slogan_last_seen_lte_1h);
        } else {
            text = mContext.getString(R.string.ui_main_peer_slogan_last_seen_gt_1h);
        }

        mLastSeenTextView.setText(text);

    }
}