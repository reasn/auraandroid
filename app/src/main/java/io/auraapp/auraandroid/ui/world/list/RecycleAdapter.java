package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.SloganComparator;
import io.auraapp.auraandroid.ui.world.PeerSlogan;
import io.auraapp.auraandroid.ui.world.list.item.ItemViewHolder;
import io.auraapp.auraandroid.ui.world.list.item.ListItem;
import io.auraapp.auraandroid.ui.world.list.item.MySlogansHeadingHolder;
import io.auraapp.auraandroid.ui.world.list.item.MySlogansHeadingItem;
import io.auraapp.auraandroid.ui.world.list.item.PeerItemHolder;
import io.auraapp.auraandroid.ui.world.list.item.PeerSloganListItem;
import io.auraapp.auraandroid.ui.world.list.item.PeersHeadingHolder;
import io.auraapp.auraandroid.ui.world.list.item.PeersHeadingItem;
import io.auraapp.auraandroid.ui.world.list.item.StatusHolder;
import io.auraapp.auraandroid.ui.world.list.item.StatusItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class RecycleAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private static final String TAG = "@aura/" + RecycleAdapter.class.getSimpleName();

    private static final int TYPE_STATUS = 141;
    private static final int TYPE_MY_SLOGANS_HEADING = 142;
    private static final int TYPE_PEERS_HEADING = 143;
    private final static int TYPE_PEER_SLOGAN = 146;

    private final LayoutInflater mInflater;
    private final Timer timer = new Timer(new Handler());
    private final Context mContext;

    private final List<ListItem> mItems;
    private final RecyclerView mListView;

    private final CollapseExpandHandler collapseExpandHandler = new CollapseExpandHandler() {
        @Override
        public void flip(ListItem item) {
            if (item == null) {
                return;
            }

            for (int i = 0; i < mItems.size(); i++) {
                ListItem candidate = mItems.get(i);
                if (candidate.mExpanded && !candidate.equals(item)) {
                    v(TAG, "Collapsing other item at index %d", i);
                    candidate.mExpanded = false;
                    notifyItemChanged(i);
                }
            }
            item.mExpanded = !item.mExpanded;
            notifyItemChanged(mItems.indexOf(item));
            mListView.smoothScrollToPosition(mItems.indexOf(item));

        }
    };

    public RecycleAdapter(@NonNull Context context, List<ListItem> items, RecyclerView listView) {
        super();
        mContext = context;
        mItems = items;
        mListView = listView;
        mInflater = LayoutInflater.from(context);
    }

    public void notifyPeerSloganListChanged(TreeMap<String, PeerSlogan> peerSloganMap) {
        d(TAG, "Updating list, peer slogans: %d", peerSloganMap.size());

        final List<ListItem> newItems = new ArrayList<>();
        for (PeerSlogan peerSlogan : peerSloganMap.values()) {
            newItems.add(new PeerSloganListItem(peerSlogan.mSlogan, peerSlogan.mPeers));
        }
        SloganComparator c = new SloganComparator();
        ListSynchronizer.syncLists(
                mItems,
                newItems,
                this,
                existingItem -> existingItem instanceof PeerSloganListItem,
                existingItem -> existingItem instanceof PeerSloganListItem,
                (item, newItem) ->
                        item instanceof PeerSloganListItem
                                && newItem instanceof PeerSloganListItem
                                && c.compare(((PeerSloganListItem) item).getSlogan(), ((PeerSloganListItem) newItem).getSlogan()) > 0
        );
    }

    public void notifyListItemChanged(ListItem item) {
        int index = mItems.indexOf(item);
        v(TAG, "notifyListItemChanged at %d, item: %s", index, item);
        notifyItemChanged(index);
    }

    /**
     * Ensures that the lastFetch information is properly reflected in items
     */
    public void onResume() {
        timer.setSerializedInterval("redraw", () -> {
            long now = System.currentTimeMillis();
            for (ListItem item : mItems) {
                if (item instanceof StatusItem) {
                    notifyItemChanged(mItems.indexOf(item));
                } else if (item instanceof PeerSloganListItem && now - ((PeerSloganListItem) item).getLastSeen() > 10000) {
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
        switch (viewType) {
            case TYPE_STATUS:
                return new StatusHolder(
                        mContext,
                        mInflater.inflate(R.layout.world_list_item_status, parent, false)
                );

            case TYPE_MY_SLOGANS_HEADING:
                return new MySlogansHeadingHolder(
                        mInflater.inflate(R.layout.world_list_item_heading, parent, false),
                        mContext);

            case TYPE_PEERS_HEADING:
                return new PeersHeadingHolder(
                        mInflater.inflate(R.layout.world_list_item_heading, parent, false),
                        mContext
                );

            case TYPE_PEER_SLOGAN:
                return new PeerItemHolder(
                        mInflater.inflate(R.layout.world_list_item_peer_slogan, parent, false),
                        mContext,
                        collapseExpandHandler);

            default:
                throw new RuntimeException("Invalid viewType " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.setItem(mItems.get(position));

        if (holder instanceof PeerItemHolder) {
//            Random rnd = new Random();
//            int color = Color.argb(
//                    255,
//                    rnd.nextInt(100) + 156,
//                    rnd.nextInt(100) + 156,
//                    rnd.nextInt(100) + 156);
//            holder.itemView.setBackgroundColor(color);

            // Alternating colors
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.gray));
            } else {
                holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.white));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        ListItem item = mItems.get(position);
        if (item instanceof StatusItem) {
            return TYPE_STATUS;
        }
        if (item instanceof MySlogansHeadingItem) {
            return TYPE_MY_SLOGANS_HEADING;
        }
        if (item instanceof PeersHeadingItem) {
            return TYPE_PEERS_HEADING;
        }

        return TYPE_PEER_SLOGAN;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}

