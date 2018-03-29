package io.auraapp.auraandroid.ui.world.list.item;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.world.list.RecycleAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.e;

public class PeerItemHolder extends ItemViewHolder {

    private static final String TAG = "aura/list/" + PeerItemHolder.class.getSimpleName();
    private final TextView mSloganTextView;
    private final TextView mLastSeenTextView;
    private final Context mContext;
    private final boolean mExpanded;
    private final LinearLayout mExpandedWrapper;

    public PeerItemHolder(View itemView,
                          Context context,
                          boolean expanded,
                          RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);

        mContext = context;
        mExpanded = expanded;
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        mLastSeenTextView = itemView.findViewById(R.id.lastSeen);
        mExpandedWrapper = itemView.findViewById(R.id.expanded_wrapper);

        itemView.setOnClickListener($ -> collapseExpandHandler.flip(getLastBoundItem()));
    }

    @Override
    public void bind(ListItem item, View itemView) {

        if (item == null) {
            e(TAG, "Trying to bind %s to null ListItem", PeerItemHolder.class.getSimpleName());
            return;
        }
        if (!(item instanceof PeerSloganListItem)) {
            e(TAG, "Trying to bind %s with %s", PeerItemHolder.class.getSimpleName(), item.getClass().getSimpleName());
            return;
        }

        PeerSloganListItem castItem = (PeerSloganListItem) item;

        mSloganTextView.setText(castItem.getSlogan().getText());

        if (!mExpanded) {
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