package io.auraapp.auraandroid.main.list;

import android.content.Context;
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
import io.auraapp.auraandroid.main.PeerSlogan;
import io.auraapp.auraandroid.main.SloganComparator;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class RecycleAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private static final String TAG = "@aura/" + RecycleAdapter.class.getSimpleName();

    private final static int TYPE_MY_COLLAPSED = 144;
    private final static int TYPE_MY_EXPANDED = 145;
    private final static int TYPE_PEER_COLLAPSED = 146;
    private final static int TYPE_PEER_EXPANDED = 147;

    private final LayoutInflater mInflater;
    private final Context mContext;

    private final List<ListItem> mItems;
    private final RecyclerView mListView;

    public static RecycleAdapter create(@NonNull Context context, RecyclerView listView) {
        return new RecycleAdapter(context, new ArrayList<>(), listView);
    }

    private RecycleAdapter(@NonNull Context context, List<ListItem> items, RecyclerView listView) {
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
            newItems.add(new ListItem(peerSlogan.mSlogan, peerSlogan.mPeers));
        }
        SloganComparator c = new SloganComparator();
        ListSynchronizer.syncLists(
                mItems,
                newItems,
                this,
                (ListItem item) -> !item.isMine(),
                (ListItem item, ListItem newItem) -> c.compare(item.getSlogan(), newItem.getSlogan()) > 0
        );
    }

    public void notifyMySlogansChanged(TreeSet<Slogan> mySlogans) {
        d(TAG, "Updating list, mySlogans: %d", mySlogans.size());

        final List<ListItem> newItems = new ArrayList<>();
        for (Slogan mySlogan : mySlogans) {
            newItems.add(new ListItem(mySlogan, null));
        }
        ListSynchronizer.syncLists(
                mItems,
                newItems,
                this,
                ListItem::isMine,
                (ListItem item, ListItem newItem) -> item.isMine() || item.compareIndex(newItem) > 0
        );
    }

    public void notifyListItemChanged(ListItem item) {
        notifyItemChanged(mItems.indexOf(item));
    }

    private final CollapseExpandHandler collapseExpandHandler = new CollapseExpandHandler() {
        @Override
        public void flip(ListItem item) {
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
            case TYPE_MY_COLLAPSED:
            case TYPE_PEER_COLLAPSED:
                return new CollapsedHolder(
                        mInflater.inflate(R.layout.list_item_collapsed, parent, false),
                        collapseExpandHandler);

            case TYPE_MY_EXPANDED:
                return new MyExpandedHolder(
                        mInflater.inflate(R.layout.list_item_my_expanded, parent, false),
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
        return item.isMine()
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

