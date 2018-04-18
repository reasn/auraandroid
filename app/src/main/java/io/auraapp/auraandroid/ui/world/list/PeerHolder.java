package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.MonoSpaceText;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PeerHolder extends ItemViewHolder {

    private static final String TAG = "@aura/list/" + PeerHolder.class.getSimpleName();
    private final TextView mNameView;
    private final LinearLayout mHeadingView;
    private final Context mContext;
    private final OnAdoptCallback mOnAdoptCallback;
    private final MonoSpaceText mTextView;
    private final TextView mStatsView;
    private final View mDetailsView;
    private final ProgressBar mSpinner;
    private RecyclerView mSlogansListView;

    public PeerHolder(View itemView,
                      Context context,
                      RecyclerAdapter.CollapseExpandHandler collapseExpandHandler,
                      OnAdoptCallback onAdoptCallback) {
        super(itemView);

        mContext = context;
        mOnAdoptCallback = onAdoptCallback;
        mSpinner = itemView.findViewById(R.id.world_peer_spinner);
        mNameView = itemView.findViewById(R.id.world_peer_name);
        mHeadingView = itemView.findViewById(R.id.world_peer_heading);
        mDetailsView = itemView.findViewById(R.id.world_peer_details);
        mTextView = itemView.findViewById(R.id.world_peer_text);
        mStatsView = itemView.findViewById(R.id.world_peer_stats);
        mSlogansListView = itemView.findViewById(R.id.world_peer_slogans_list);
        mSlogansListView.setNestedScrollingEnabled(false);
        itemView.setOnClickListener($ -> collapseExpandHandler.flip(getLastBoundItem()));
    }

    @Override
    public void bind(ListItem item, View itemView) {
        v(TAG, "Binding list item view");
        if (item == null) {
            e(TAG, "Trying to bind %s to null ListItem", PeerHolder.class.getSimpleName());
            return;
        }
        if (!(item instanceof PeerItem)) {
            e(TAG, "Trying to bind %s with %s", PeerHolder.class.getSimpleName(), item.getClass().getSimpleName());
            return;
        }

        PeerItem castItem = (PeerItem) item;
        Peer peer = castItem.getPeer();

        ColorSet colorSet = ColorSet.create(peer.mColor != null ? peer.mColor : "#ffffff");
        bindProfile(castItem, colorSet);
        bindSlogans(peer, colorSet);
        bindStats(peer, colorSet);
    }

    private void bindProfile(PeerItem item, ColorSet colorSet) {

        Peer peer = item.getPeer();

        mHeadingView.setBackgroundColor(colorSet.mBackground);

        mSpinner.setVisibility(peer.mSynchronizing
                ? View.VISIBLE
                : View.GONE);

        mNameView.setText(peer.mName == null
                ? mContext.getString(R.string.world_peer_no_name)
                : peer.mName);
        mNameView.setTextColor(colorSet.mText);

        mDetailsView.setVisibility(item.mExpanded
                ? View.VISIBLE
                : View.GONE);

        // EditTexts keep their state and might ignore setText without this setting
        mTextView.setText(peer.mText);
        mTextView.setBackgroundColor(colorSet.mBackground);
        mTextView.setTextColor(colorSet.mText);
        mDetailsView.setBackgroundColor(colorSet.mBackground);
    }

    private void bindSlogans(Peer peer, ColorSet colorSet) {

        // TODO avoid recreation on each change, e.g. cache stuff on Peer

        ArrayList<PeerSloganItem> items = new ArrayList<>();
        for (Slogan slogan : peer.mSlogans) {
            items.add(new PeerSloganItem(slogan.getText(), slogan));
        }

        PeerSloganAdapter adapter = new PeerSloganAdapter(
                mContext,
                mSlogansListView,
                colorSet,
                mOnAdoptCallback,
                items
        );

        mSlogansListView.setBackgroundColor(colorSet.mAccentBackground);
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
        mStatsView.setTextColor(colorSet.mText);
    }
}