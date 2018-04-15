package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.e;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PeerItemHolder extends ItemViewHolder {

    static class ColorSet {
        @ColorInt
        int mBackground;
        @ColorInt
        int mText;
        @ColorInt
        int mAccentBackground;
        @ColorInt
        int mAccentText;

        private ColorSet(int background, int text, int accentBackground, int accentText) {
            mBackground = background;
            mText = text;
            mAccentBackground = accentBackground;
            mAccentText = accentText;
        }

        static ColorSet create(@NonNull String color) {
            int background = Color.parseColor(color);
            int accentBackground = ColorHelper.getAccent(background);

            return new ColorSet(
                    background,
                    ColorHelper.getTextColor(background),
                    accentBackground,
                    ColorHelper.getTextColor(accentBackground)
            );
        }
    }

    private static final String TAG = "aura/list/" + PeerItemHolder.class.getSimpleName();
    private final TextView mNameView;
    private final Context mContext;
    private final OnAdoptCallback mOnAdoptCallback;
    private final EditText mTextView;
    private final TextView mStatsView;
    private final View mDetailsView;
    private ListView mSlogansListView;

    public PeerItemHolder(View itemView,
                          Context context,
                          RecyclerAdapter.CollapseExpandHandler collapseExpandHandler, OnAdoptCallback onAdoptCallback) {
        super(itemView);

        mContext = context;
        mOnAdoptCallback = onAdoptCallback;
        mNameView = itemView.findViewById(R.id.world_peer_item_name);
        mDetailsView = itemView.findViewById(R.id.world_peer_item_details);
        mTextView = itemView.findViewById(R.id.world_peer_item_text);
        mStatsView = itemView.findViewById(R.id.world_peer_item_stats);
        mSlogansListView = itemView.findViewById(R.id.world_peer_item_slogans_list);

        itemView.setOnClickListener($ -> collapseExpandHandler.flip(getLastBoundItem()));
    }

    @Override
    public void bind(ListItem item, View itemView) {
        v(TAG, "Binding list item view");
        if (item == null) {
            e(TAG, "Trying to bind %s to null ListItem", PeerItemHolder.class.getSimpleName());
            return;
        }
        if (!(item instanceof PeerItem)) {
            e(TAG, "Trying to bind %s with %s", PeerItemHolder.class.getSimpleName(), item.getClass().getSimpleName());
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

        mNameView.setText(peer.mName);
        mNameView.setBackgroundColor(colorSet.mBackground);
        mNameView.setTextColor(colorSet.mText);

        mDetailsView.setVisibility(item.mExpanded
                ? View.VISIBLE
                : View.GONE);

        // EditTexts keep their state and might ignore setText without this setting
        mTextView.setSaveEnabled(false);
        mTextView.setText(peer.mText);
        mTextView.setBackgroundColor(colorSet.mBackground);
        mTextView.setTextColor(colorSet.mText);
        mDetailsView.setBackgroundColor(colorSet.mBackground);
    }

    private void bindSlogans(Peer peer, ColorSet colorSet) {

        // TODO avoid recreation on each change, e.g. cache stuff on Peer

        PeerSloganListAdapter adapter = new PeerSloganListAdapter(
                mContext,
                R.layout.world_peer_item_slogan,
                colorSet,
                peer.mSlogans
        );

        mSlogansListView.setBackgroundColor(colorSet.mAccentBackground);
        mSlogansListView.setNestedScrollingEnabled(false);
        mSlogansListView.setAdapter(adapter);
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