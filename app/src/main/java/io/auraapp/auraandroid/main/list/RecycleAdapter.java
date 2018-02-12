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

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class RecycleAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private final static int TYPE_MY_COLLAPSED = 144;
    private final static int TYPE_MY_EXPANDED = 145;
    private final static int TYPE_PEER_COLLAPSED = 146;
    private final static int TYPE_PEER_EXPANDED = 147;
    private final OnEditHandler mOnEditHandler;
    private final OnAdoptHandler mOnAdoptHandler;
    private final OnDropHandler mOnDropHandler;

    @FunctionalInterface
    public interface OnClickHandler {
        void onClick(ListItem item);
    }

    @FunctionalInterface
    public interface OnAdoptHandler {
        void onAdopt(Slogan slogan);
    }

    @FunctionalInterface
    public interface OnEditHandler {
        void onEdit(Slogan slogan);
    }

    @FunctionalInterface
    public interface OnDropHandler {
        void onDrop(Slogan slogan);
    }

    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private final LayoutInflater mInflater;
    private static final String TAG = "@aura/" + RecycleAdapter.class.getSimpleName();
    private final TreeSet<Slogan> mMySlogans;
    private final TreeSet<Slogan> mPeerSlogans;

    private final List<ListItem> mItems;

    public static RecycleAdapter create(@NonNull Context context,
                                        TreeSet<Slogan> mySlogans,
                                        TreeSet<Slogan> peerSlogans,
                                        OnAdoptHandler onAdoptHandler,
                                        OnEditHandler onEditHandler,
                                        OnDropHandler onDropHandler) {
        return new RecycleAdapter(context, new ArrayList<>(), mySlogans, peerSlogans, onAdoptHandler, onEditHandler, onDropHandler);
    }

    private RecycleAdapter(@NonNull Context context,
                           List<ListItem> items,
                           TreeSet<Slogan> mySlogans,
                           TreeSet<Slogan> peerSlogans,
                           OnAdoptHandler onAdoptHandler,
                           OnEditHandler onEditHandler,
                           OnDropHandler onDropHandler) {
        super();
        mItems = items;
        mMySlogans = mySlogans;
        mPeerSlogans = peerSlogans;
        mOnAdoptHandler = onAdoptHandler;
        mOnDropHandler = onDropHandler;
        mOnEditHandler = onEditHandler;
        mInflater = LayoutInflater.from(context);
    }

    public void notifyDataSetChanged2() {
        d(TAG, "Updating list, mySlogans: %d, peerSlogans: %d", mMySlogans.size(), mPeerSlogans.size());
        mItems.clear();
// TODO needs to become way more efficient
        for (Slogan mySlogan : mMySlogans) {
            mItems.add(new ListItem(mySlogan, true));
        }
        for (Slogan peerSlogan : mPeerSlogans) {
            mItems.add(new ListItem(peerSlogan, false));
        }
//        notifyItemInserted(mItems.size());
        notifyDataSetChanged();
    }

    private final CollapseExpandHandler collapseExpandHandler = new CollapseExpandHandler() {
        @Override
        public void flip(ListItem item) {
            item.mExpanded = !item.mExpanded;
            notifyItemChanged(mItems.indexOf(item));
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
                        collapseExpandHandler,
                        mOnEditHandler,
                        mOnDropHandler);

            case TYPE_PEER_EXPANDED:
                return new PeerExpandedHolder(
                        mInflater.inflate(R.layout.list_item_peer_expanded, parent, false),
                        collapseExpandHandler,
                        mOnAdoptHandler);

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
                        ? TYPE_MY_EXPANDED
                        : TYPE_MY_COLLAPSED);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}

