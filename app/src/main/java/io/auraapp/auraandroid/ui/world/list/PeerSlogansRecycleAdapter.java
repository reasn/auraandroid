package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.SloganComparator;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.ListSynchronizer;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapter;
import io.auraapp.auraandroid.ui.world.PeerSlogan;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class PeerSlogansRecycleAdapter extends RecyclerAdapter {

    private static final String TAG = "@aura/" + PeerSlogansRecycleAdapter.class.getSimpleName();

    private final static int TYPE_PEER_SLOGAN = 144;
    private final OnAdoptCallback mOnAdoptCallback;

    public PeerSlogansRecycleAdapter(@NonNull Context context, RecyclerView listView, OnAdoptCallback onAdoptCallback) {
        super(context, listView);
        mOnAdoptCallback = onAdoptCallback;
    }

    public void notifyPeerSloganListChanged(TreeMap<String, PeerSlogan> peerSloganMap) {
        d(TAG, "Updating list, peer slogans: %d", peerSloganMap.size());

        final List<ListItem> newItems = new ArrayList<>();
        for (PeerSlogan peerSlogan : peerSloganMap.values()) {
            newItems.add(new PeerSloganListItem(peerSlogan.mSlogan, peerSlogan.mPeers));
        }
        SloganComparator c = new SloganComparator();
        // TODO compare by frequency and recency
        ListSynchronizer.syncLists(
                mItems,
                newItems,
                this,
                (item, newItem) -> item.compareIndex(newItem) > 0
        );
    }

    private Timer.Timeout mRedrawTimeout;

    /**
     * Ensures that the lastFetch information is properly reflected in items
     */
    public void onResume() {
        Timer.clear(mRedrawTimeout);
        mRedrawTimeout = timer.setSerializedInterval(() -> {
            long now = System.currentTimeMillis();
            for (ListItem item : mItems) {
                if (now - ((PeerSloganListItem) item).getLastSeen() > 10000) {
                    notifyItemChanged(mItems.indexOf(item));
                }
            }
        }, 1000);
    }

    public void onPause() {
        Timer.clear(mRedrawTimeout);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PeerSloganItemHolder(
                mInflater.inflate(R.layout.world_list_item_peer_slogan, parent, false),
                mContext,
                collapseExpandHandler,
                mOnAdoptCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        // Alternating colors
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.gray));
        } else {
            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.white));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_PEER_SLOGAN;
    }
}

