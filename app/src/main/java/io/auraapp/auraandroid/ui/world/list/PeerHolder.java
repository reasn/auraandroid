package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;

import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PeerHolder extends ExpandableViewHolder {

    private static final String TAG = "@aura/list/" + PeerHolder.class.getSimpleName();
    private final TextView mNameView;
    private final LinearLayout mHeadingView;
    private final Context mContext;
    private final OnAdoptCallback mOnAdoptCallback;
    private final TextView mTextView;
    private final TextView mStatsView;
    private final View mDetailsView;
    private final ProgressBar mSpinner;
    private RecyclerView mSlogansListView;
    private PeerSloganHolder.WhatsMyColorCallback mWhatsMyColorCallback;

    PeerHolder(View itemView,
               Context context,
               OnAdoptCallback onAdoptCallback,
               PeerSloganHolder.WhatsMyColorCallback whatsMyColorCallback) {
        super(itemView);

        mContext = context;
        mOnAdoptCallback = onAdoptCallback;
        mWhatsMyColorCallback = whatsMyColorCallback;
        mSpinner = itemView.findViewById(R.id.world_peer_spinner);
        mNameView = itemView.findViewById(R.id.world_peer_name);
        mHeadingView = itemView.findViewById(R.id.world_peer_heading);
        mDetailsView = itemView.findViewById(R.id.world_peer_details);
        mTextView = itemView.findViewById(R.id.world_peer_text);
        mStatsView = itemView.findViewById(R.id.world_peer_stats);
        mSlogansListView = itemView.findViewById(R.id.world_peer_slogans_list);
        mSlogansListView.setNestedScrollingEnabled(false);
        // TODO is that the case? untested!
        // Supresses scrolling on dataset changes
        // Thanks https://stackoverflow.com/questions/31860185/nested-recyclerview-scrolls-to-the-first-item-on-notifyitemchanged
        mSlogansListView.setItemAnimator(null);
    }

    public void setPool(RecyclerView.RecycledViewPool pool) {
        mSlogansListView.setRecycledViewPool(pool);
    }

    @Override
    public void bind(Object item, boolean expanded, View.OnClickListener collapseExpandHandler) {

        v(TAG, "Binding list item view");

        if (item == null) {
            e(TAG, "Trying to bind %s to null item", PeerHolder.class.getSimpleName());
            return;
        }
        if (!(item instanceof Peer)) {
            e(TAG, "Trying to bind %s with %s", PeerHolder.class.getSimpleName(), item.getClass().getSimpleName());
            return;
        }

        itemView.setActivated(expanded);
        itemView.setOnClickListener(collapseExpandHandler);

        Peer peer = (Peer) item;

        ColorSet colorSet = ColorSet.create(peer.mColor != null ? peer.mColor : "#ffffff");
        bindProfile(peer, colorSet, expanded);
        bindSlogans(peer, colorSet);
        bindStats(peer, colorSet);
    }

    private void bindProfile(Peer peer, ColorSet colorSet, boolean expanded) {

        mHeadingView.setBackgroundColor(colorSet.mBackground);

        mSpinner.setVisibility(peer.mSynchronizing
                ? View.VISIBLE
                : View.GONE);

        mNameView.setText(peer.mName == null
                ? mContext.getString(R.string.world_peer_no_name)
                : peer.mName);
        mNameView.setTextColor(colorSet.mText);

        mDetailsView.setVisibility(expanded
                ? View.VISIBLE
                : View.GONE);

        // EditTexts keep their state and might ignore setText without this setting
        mTextView.setText(peer.mText);
        mTextView.setBackgroundColor(colorSet.mBackground);
        mTextView.setTextColor(colorSet.mText);
        mTextView.setLinkTextColor(colorSet.mText);
        mDetailsView.setBackgroundColor(colorSet.mBackground);
    }

    // Has nothing to do in class contest but Java doesn't allow to closure scope local variables
    private boolean mOrderChanged;

    private void bindSlogans(Peer peer, ColorSet colorSet) {

        mSlogansListView.setBackgroundColor(colorSet.mAccentBackground);

        if (mSlogansListView.getAdapter() != null) {

            PeerSloganAdapter adapter = (PeerSloganAdapter) mSlogansListView.getAdapter();

            List<Object> existingSlogans = adapter.getItems();

            if (!colorSet.equals(adapter.mColorSet)) {
                adapter.mColorSet = colorSet;
                adapter.notifyDataSetChanged();

            } else {
                DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new SlogansDiffCallback(existingSlogans, peer.mSlogans));
                existingSlogans.clear();
                existingSlogans.addAll(peer.mSlogans);

                mOrderChanged = false;
                diff.dispatchUpdatesTo(new ListUpdateCallback() {
                    @Override
                    public void onInserted(int position, int count) {
                        mOrderChanged = true;
                    }

                    @Override
                    public void onRemoved(int position, int count) {
                        mOrderChanged = true;
                    }

                    @Override
                    public void onMoved(int fromPosition, int toPosition) {
                        mOrderChanged = true;
                    }

                    @Override
                    public void onChanged(int position, int count, Object payload) {
                    }
                });

                // To maintain alternating colors require that all items be redrawn
                if (mOrderChanged) {
                    adapter.notifyDataSetChanged();
                } else {
                    diff.dispatchUpdatesTo(mSlogansListView.getAdapter());
                }
            }
            return;
        }

        PeerSloganAdapter adapter = new PeerSloganAdapter(
                mContext,
                mSlogansListView,
                colorSet,
                mOnAdoptCallback,
                peer.mSlogans,
                mWhatsMyColorCallback
        );

        mSlogansListView.setAdapter(adapter);
        mSlogansListView.setLayoutManager(new LinearLayoutManager(mContext));
    }

    private void bindStats(Peer peer, ColorSet colorSet) {
        long lastSeen = peer.mLastSeenTimestamp;
        long elapsedSeconds = Math.round((System.currentTimeMillis() - lastSeen) / 1000);

        String text;
        if (elapsedSeconds < 10) {
            text = mContext.getString(R.string.world_peer_last_seen_lt_10s);
        } else if (elapsedSeconds < 60) {
            text = mContext.getString(R.string.world_peer_last_seen_lt_1min);
        } else if (elapsedSeconds < 10 * 60) {
            text = mContext.getString(R.string.world_peer_last_seen_lt_10min)
                    .replace("##elapsed_minutes##", (int) Math.ceil(elapsedSeconds / 60) + "");
        } else if (elapsedSeconds < 30 * 60) {
            text = mContext.getString(R.string.world_peer_last_seen_lt_30min);
        } else if (elapsedSeconds < 45 * 60) {
            text = mContext.getString(R.string.world_peer_last_seen_lt_45min);
        } else if (elapsedSeconds < 61 * 60) {
            text = mContext.getString(R.string.world_peer_last_seen_lte_1h);
        } else {
            text = mContext.getString(R.string.world_peer_last_seen_gt_1h);
        }

        mStatsView.setText(text);
        mStatsView.setBackgroundColor(colorSet.mBackground);
        mStatsView.setTextColor(ColorHelper.getAccent(colorSet.mText));
    }

}