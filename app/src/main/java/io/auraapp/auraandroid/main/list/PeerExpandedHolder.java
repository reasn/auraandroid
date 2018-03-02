package io.auraapp.auraandroid.main.list;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;

import static io.auraapp.auraandroid.common.FormattedLog.e;

class PeerExpandedHolder extends ItemViewHolder {

    private static final String TAG = "aura/list/" + PeerExpandedHolder.class.getSimpleName();
    private final TextView mSloganTextView;
    private final TextView mLastSeenTextView;
    private final Context mContext;

    PeerExpandedHolder(View itemView, Context context, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);

        mContext = context;

        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        mLastSeenTextView = itemView.findViewById(R.id.lastSeen);

        itemView.setOnClickListener((v) -> collapseExpandHandler.flip(mItem));
    }

    @Override
    void bind(ListItem item) {

        if (item == null) {
            e(TAG, "Trying to bind %s to null ListItem", PeerExpandedHolder.class.getSimpleName());
            return;
        }
        if (!(item instanceof PeerSloganListItem)) {
            e(TAG, "Trying to bind %s with %s", PeerExpandedHolder.class.getSimpleName(), item.getClass().getSimpleName());
            return;
        }
        PeerSloganListItem castItem = (PeerSloganListItem) item;

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

        mSloganTextView.setText(castItem.getSlogan().getText());
    }
}