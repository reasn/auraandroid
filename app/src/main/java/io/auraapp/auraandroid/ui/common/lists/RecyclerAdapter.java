package io.auraapp.auraandroid.ui.common.lists;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.common.Timer;

public abstract class RecyclerAdapter extends RecyclerView.Adapter<ExpandableViewHolder> {

    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private static final String TAG = "@aura/" + RecyclerAdapter.class.getSimpleName();

    protected final LayoutInflater mInflater;
    protected final Timer timer = new Timer(new Handler());
    protected final Context mContext;

    protected final List<Object> mItems = new ArrayList<>();
    private final RecyclerView mListView;
//
//    protected final CollapseExpandHandler mCollapseExpandHandler = item -> {
//        if (item == null) {
//            return;
//        }
//
//        for (int i = 0; i < mItems.size(); i++) {
//            ItemType candidate = mItems.get(i);
//            if (candidate.mExpanded && !candidate.equals(item)) {
//                v(TAG, "Collapsing other item at index %d", i);
//                candidate.mExpanded = false;
//                notifyItemChanged(i);
//            }
//        }
//
//        int position = mItems.indexOf(item);
//        if (item.mExpanded) {
//            v(TAG, "Collapsing item at index %d", position);
//        } else {
//            v(TAG, "Expanding item at index %d", position);
//        }
//        item.mExpanded = !item.mExpanded;
//        notifyItemChanged(position);
////            mListView.smoothScrollToPosition(mItems.indexOf(item));
//    };

    public RecyclerAdapter(@NonNull Context context, RecyclerView listView) {
        super();
        if (listView == null) {
            throw new RuntimeException("Provided list view is null");
        }
        mContext = context;
        mListView = listView;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpandableViewHolder holder, int position) {
//        holder.update(mItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}

