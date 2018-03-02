package io.auraapp.auraandroid.main.list;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.main.PeerSlogan;
import io.auraapp.auraandroid.main.SloganComparator;
import io.auraapp.auraandroid.main.list.item.ItemViewHolder;
import io.auraapp.auraandroid.main.list.item.ListItem;
import io.auraapp.auraandroid.main.list.item.MyCollapsedHolder;
import io.auraapp.auraandroid.main.list.item.MyExpandedHolder;
import io.auraapp.auraandroid.main.list.item.MySloganListItem;
import io.auraapp.auraandroid.main.list.item.PeerCollapsedHolder;
import io.auraapp.auraandroid.main.list.item.PeerExpandedHolder;
import io.auraapp.auraandroid.main.list.item.PeerSloganListItem;
import io.auraapp.auraandroid.main.list.item.StatusHolder;
import io.auraapp.auraandroid.main.list.item.StatusItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class RecycleAdapter extends RecyclerView.Adapter<ItemViewHolder> {


    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private static final String TAG = "@aura/" + RecycleAdapter.class.getSimpleName();

    private static final int TYPE_STATUS_COLLAPSED = 140;
    private static final int TYPE_STATUS_EXPANDED = 141;
    private static final int TYPE_PEERS_STATE_COLLAPSED = 142;
    private static final int TYPE_PEERS_STATE_EXPANDED = 143;
    private final static int TYPE_MY_COLLAPSED = 144;
    private final static int TYPE_MY_EXPANDED = 145;
    private final static int TYPE_PEER_COLLAPSED = 146;
    private final static int TYPE_PEER_EXPANDED = 147;

    private final LayoutInflater mInflater;
    private final Timer timer = new Timer(new Handler());
    private final Context mContext;

    private final List<ListItem> mItems;
    private final RecyclerView mListView;

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
                item -> item instanceof PeerSloganListItem,
                (item, newItem) ->
                        item instanceof PeerSloganListItem
                                && newItem instanceof PeerSloganListItem
                                && c.compare(((PeerSloganListItem) item).getSlogan(), ((PeerSloganListItem) newItem).getSlogan()) > 0
        );
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

    public void notifyMySlogansChanged(TreeSet<Slogan> mySlogans) {
        d(TAG, "Updating list, mySlogans: %d", mySlogans.size());

        final List<ListItem> newItems = new ArrayList<>();
        for (Slogan mySlogan : mySlogans) {
            newItems.add(new MySloganListItem(mySlogan));
        }
        ListSynchronizer.syncLists(
                mItems,
                newItems,
                this,
                item -> item instanceof MySloganListItem,
                (item, newItem) -> item instanceof MySloganListItem || item.compareIndex(newItem) > 0
        );
    }

    public void notifyListItemChanged(ListItem item) {
        notifyItemChanged(mItems.indexOf(item));
    }

    private final CollapseExpandHandler collapseExpandHandler = new CollapseExpandHandler() {
        @Override
        public void flip(ListItem item) {
            if (item == null) {
                return;
            }
            for (int i = 0; i < mItems.size(); i++) {
                ListItem candidate = mItems.get(i);
                if (candidate.mExpanded && !candidate.equals(item)) {
                    candidate.mExpanded = false;
                    notifyItemChanged(i);
                }
            }
            item.mExpanded = !item.mExpanded;
            notifyItemChanged(mItems.indexOf(item));
            mListView.smoothScrollToPosition(mItems.indexOf(item));
        }
    };

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_STATUS_COLLAPSED:
                return new StatusHolder(
                        false,
                        mContext,
                        mInflater.inflate(R.layout.list_item_status_collapsed, parent, false),
                        collapseExpandHandler);

            case TYPE_STATUS_EXPANDED:
                return new StatusHolder(
                        true,
                        mContext,
                        mInflater.inflate(R.layout.list_item_status_expanded, parent, false),
                        collapseExpandHandler);

            case TYPE_MY_COLLAPSED:
                return new MyCollapsedHolder(mInflater.inflate(R.layout.list_item_collapsed, parent, false));

            case TYPE_MY_EXPANDED:
                return new MyExpandedHolder(mInflater.inflate(R.layout.list_item_my_expanded, parent, false));

            case TYPE_PEER_COLLAPSED:
                return new PeerCollapsedHolder(
                        mInflater.inflate(R.layout.list_item_collapsed, parent, false),
                        collapseExpandHandler);

            case TYPE_PEER_EXPANDED:
                return new PeerExpandedHolder(
                        mInflater.inflate(R.layout.list_item_peer_expanded, parent, false),
                        mContext,
                        collapseExpandHandler);

            default:
                throw new RuntimeException("Invalid viewType " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.setItem(mItems.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        ListItem item = mItems.get(position);
        if (item instanceof StatusItem) {
            return item.mExpanded
                    ? TYPE_STATUS_EXPANDED
                    : TYPE_STATUS_COLLAPSED;
        }

        return item instanceof MySloganListItem
                ? (
                item.mExpanded
                        ? TYPE_MY_EXPANDED
                        : TYPE_MY_COLLAPSED)
                : (
                item.mExpanded
                        ? TYPE_PEER_EXPANDED
                        : TYPE_PEER_COLLAPSED);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}

