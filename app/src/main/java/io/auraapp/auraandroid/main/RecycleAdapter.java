package io.auraapp.auraandroid.main;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;

import static io.auraapp.auraandroid.common.FormattedLog.d;

class RecycleAdapter extends RecyclerView.Adapter<RecycleViewHolder> {

    private final OnClickHandler mOnClickHandler;

    @FunctionalInterface
    public interface OnClickHandler {
        void onClick(ListItem item);
    }

    private final LayoutInflater mInflater;
    private static final String TAG = "@aura/" + RecycleAdapter.class.getSimpleName();
    private final TreeSet<Slogan> mMySlogans;
    private final TreeSet<Slogan> mPeerSlogans;

    private final List<ListItem> mItems;

    static RecycleAdapter create(@NonNull Context context, TreeSet<Slogan> mySlogans, TreeSet<Slogan> peerSlogans, OnClickHandler onClickHandler) {
        return new RecycleAdapter(context, new ArrayList<>(), mySlogans, peerSlogans, onClickHandler);
    }

    private RecycleAdapter(@NonNull Context context, List<ListItem> items, TreeSet<Slogan> mySlogans, TreeSet<Slogan> peerSlogans, OnClickHandler onClickHandler) {
        super();
        mItems = items;
        mMySlogans = mySlogans;
        mPeerSlogans = peerSlogans;
        mInflater = LayoutInflater.from(context);
        mOnClickHandler = onClickHandler;
    }

    void notifyDataSetChanged2() {
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

    @Override
    public RecycleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View thisItemsView = mInflater.inflate(R.layout.list_item, parent, false);
        return new RecycleViewHolder(thisItemsView, mOnClickHandler);
    }

    @Override
    public void onBindViewHolder(RecycleViewHolder holder, int position) {
        // Find out the data, based on this view holder's position
        ListItem item = mItems.get(position);
        holder.mSloganTextView.setText(item.getSlogan().getText());
        holder.mItem = item;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

}

