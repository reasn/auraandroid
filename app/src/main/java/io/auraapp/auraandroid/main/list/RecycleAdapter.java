package io.auraapp.auraandroid.main.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.main.PeerSlogan;
import io.auraapp.auraandroid.main.SloganComparator;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;

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

    public void notifyPeerSloganChanged(PeerSlogan slogan) {
        for (ListItem item : mItems) {
            if (item.getSlogan().equals(slogan.mSlogan)) {
                int index = mItems.indexOf(item);
                d(TAG, "Peer slogan changed, index: %d, peerSlogan: %s", index, slogan);
                notifyItemChanged(index);
                break;
            }
        }
    }

    public void notifyPeerSlogansChanged(TreeMap<String, PeerSlogan> mSloganGroupMap) {
        // TODO implement
    }

    public void notifyMySlogansChanged(TreeSet<Slogan> mySlogans) {
        d(TAG, "Updating list, mySlogans: %d", mySlogans.size());

        final List<ListItem> newItems = new ArrayList<>();
        for (Slogan mySlogan : mySlogans) {
            newItems.add(new ListItem(mySlogan, null));
        }

        Set<Runnable> mutations = new HashSet<>();

        // Remove absent items
        for (ListItem item : mItems) {
            if (!newItems.contains(item)) {
                mutations.add(() -> {
                    int index = mItems.indexOf(item);
                    v(TAG, "Removing item %s at %d", item.getSlogan(), index);
                    mItems.remove(item);
                    notifyItemRemoved(index);
                });
            }
        }

        for (Runnable r : mutations) {
            r.run();
        }
        mutations.clear();

        // Add new items
        SloganComparator c = new SloganComparator();
        for (ListItem newItem : newItems) {
            if (!mItems.contains(newItem)) {
                mutations.add(() -> {
                    int index;
                    // Determine the index of the first item that's supposed to be after newItem
                    for (index = 0; index < mItems.size(); index++) {
                        ListItem item = mItems.get(index);
                        if (!item.isMine() || c.compare(item.getSlogan(), newItem.getSlogan()) > 0) {
                            break;
                        }
                    }
                    v(TAG, "Inserting item %s at %d", newItem.getSlogan(), index);
                    mItems.add(index, newItem);
                    notifyItemInserted(index);
                });
            }
        }

        for (Runnable r : mutations) {
            r.run();
        }
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

