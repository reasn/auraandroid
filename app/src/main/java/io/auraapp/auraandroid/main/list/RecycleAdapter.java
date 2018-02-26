package io.auraapp.auraandroid.main.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.main.PeerSlogan;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class RecycleAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private final static int TYPE_MY_COLLAPSED = 144;
    private final static int TYPE_MY_EXPANDED = 145;
    private final static int TYPE_PEER_COLLAPSED = 146;
    private final static int TYPE_PEER_EXPANDED = 147;

    private final LayoutInflater mInflater;
    private static final String TAG = "@aura/" + RecycleAdapter.class.getSimpleName();
    private final TreeSet<Slogan> mMySlogans;
    private final TreeSet<PeerSlogan> mPeerSlogans;

    private final List<ListItem> mItems;
    private final RecyclerView mListView;

    public static RecycleAdapter create(@NonNull Context context,
                                        TreeSet<Slogan> mySlogans,
                                        TreeSet<PeerSlogan> peerSlogans,
                                        RecyclerView listView) {
        return new RecycleAdapter(context, new ArrayList<>(), mySlogans, peerSlogans, listView);
    }

    private RecycleAdapter(@NonNull Context context,
                           List<ListItem> items,
                           TreeSet<Slogan> mySlogans,
                           TreeSet<PeerSlogan> peerSlogans,
                           RecyclerView listView) {
        super();
        mItems = items;
        mMySlogans = mySlogans;
        mPeerSlogans = peerSlogans;
        mListView = listView;
        mInflater = LayoutInflater.from(context);
    }

    public void notifySlogansChanged() {
        d(TAG, "Updating list, mySlogans: %d, peerSlogans: %d", mMySlogans.size(), mPeerSlogans.size());
        mItems.clear();
// TODO needs to become way more efficient
        for (Slogan mySlogan : mMySlogans) {
            mItems.add(new ListItem(mySlogan, null));
        }
        for (PeerSlogan peerSlogan : mPeerSlogans) {
            mItems.add(new ListItem(peerSlogan.mSlogan, peerSlogan.mPeers));
        }
//        notifyItemInserted(mItems.size());
        notifyDataSetChanged();
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

