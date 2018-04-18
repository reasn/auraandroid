package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapter;
import io.auraapp.auraandroid.ui.profile.SpacerHolder;
import io.auraapp.auraandroid.ui.profile.SpacerItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class PeerAdapter extends RecyclerAdapter {

    private static final String TAG = "@aura/" + PeerAdapter.class.getSimpleName();

    private final static int TYPE_PEER = 244;
    private final static int TYPE_SPACER = 245;
    private final OnAdoptCallback mOnAdoptCallback;
    private Timer.Timeout mRedrawTimeout;

    private final Comparator<Peer> mComparator = (a, b) -> {
        if (a.mId == b.mId) {
            // Avoids duplicates when the name changes
            return 0;
        }
        if (Math.abs(a.mLastSeenTimestamp - b.mLastSeenTimestamp) < 60000) {
            if (a.mName == null || b.mName == null) {
                return a.mId - b.mId;
            }
            return a.mName.compareTo(b.mName);
        }
        return (int) (a.mLastSeenTimestamp - b.mLastSeenTimestamp);
    };

    public PeerAdapter(@NonNull Context context, RecyclerView listView, OnAdoptCallback onAdoptCallback) {
        super(context, listView);
        mOnAdoptCallback = onAdoptCallback;
        mItems.add(new SpacerItem());
    }

    public void notifyPeersChanged(Set<Peer> peerSet) {

        TreeSet<Peer> treeSet = new TreeSet<>(mComparator);
        // Filter out peers without a name, they look weird
//        for (Peer peer : peerSet) {
//            if (peer.mName != null) {
//            treeSet.add(peer);
//            }
//        }
        treeSet.addAll(peerSet);
        ArrayList<Peer> sortedPeers = new ArrayList<>(treeSet);

        final List<ListItem> newItems = new ArrayList<>();
        for (Peer peer : sortedPeers) {
            newItems.add(new PeerItem(peer));
        }
        newItems.add(new SpacerItem());

        d(TAG, "Updating list, peers was %d, is: %d", mItems.size(), newItems.size());

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new PeersDiffCallback(mItems, newItems));
        mItems.clear();
        mItems.addAll(newItems);
        diff.dispatchUpdatesTo(this);

//        ListSynchronizer.syncLists(
//                mItems,
//                newItems,
//                this,
//                (item, newItem) -> ((PeerItem) item).getPeer().mLastSeenTimestamp > ((PeerItem) newItem).getPeer().mLastSeenTimestamp
//        );
    }

    /**
     * Ensures that the lastFetch information is properly reflected in items
     */
    public void onResume() {
        Timer.clear(mRedrawTimeout);
        mRedrawTimeout = timer.setSerializedInterval(() -> {
            long now = System.currentTimeMillis();
            for (ListItem item : mItems) {
                if (item instanceof SpacerItem) {
                    continue;
                }
                if (now - ((PeerItem) item).getPeer().mLastSeenTimestamp > 10000) {
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
        if (viewType == TYPE_SPACER) {
            return new SpacerHolder(mInflater.inflate(R.layout.common_list_spacer, parent, false));
        }
        return new PeerHolder(
                mInflater.inflate(R.layout.world_peer, parent, false),
                mContext,
                mCollapseExpandHandler,
                mOnAdoptCallback);
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof SpacerItem) {
            return TYPE_SPACER;
        }
        return TYPE_PEER;
    }
}

