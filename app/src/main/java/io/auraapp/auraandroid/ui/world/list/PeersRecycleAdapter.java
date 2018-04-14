package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.ListSynchronizer;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class PeersRecycleAdapter extends RecyclerAdapter {

    private static final String TAG = "@aura/" + PeersRecycleAdapter.class.getSimpleName();

    private final static int TYPE_PEER_SLOGAN = 144;
    private final OnAdoptCallback mOnAdoptCallback;

    public PeersRecycleAdapter(@NonNull Context context, RecyclerView listView, OnAdoptCallback onAdoptCallback) {
        super(context, listView);
        mOnAdoptCallback = onAdoptCallback;
    }

    public void notifyPeersChanged(Set<Peer> peers) {
        d(TAG, "Updating list, peer slogans: %d", peers.size());

        final List<ListItem> newItems = new ArrayList<>();
        for (Peer peer : peers) {
            newItems.add(new PeerItem(peer));
        }
        ListSynchronizer.syncLists(
                mItems,
                newItems,
                this,
                (item, newItem) -> ((PeerItem) item).getPeer().mLastSeenTimestamp > ((PeerItem) newItem).getPeer().mLastSeenTimestamp
        );
    }

    /**
     * Ensures that the lastFetch information is properly reflected in items
     */
    public void onResume() {
        timer.setSerializedInterval("redraw", () -> {
            long now = System.currentTimeMillis();
            for (ListItem item : mItems) {
                if (now - ((PeerItem) item).getPeer().mLastSeenTimestamp > 10000) {
                    notifyItemChanged(mItems.indexOf(item));
                }
            }
        }, 1000);
    }

    public void onPause() {
        timer.clear("redraw");
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PeerItemHolder(
                mInflater.inflate(R.layout.world_peer_item, parent, false),
                mContext,
                collapseExpandHandler,
                mOnAdoptCallback);
    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
//        super.onBindViewHolder(holder, position);
//        // Alternating colors
//        if (position % 2 == 0) {
//            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.gray));
//        } else {
//            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.white));
//        }
//    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_PEER_SLOGAN;
    }
}

