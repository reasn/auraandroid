package io.auraapp.auraandroid.ui.common.lists;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.common.Timer;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public abstract class RecyclerAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private static final String TAG = "@aura/" + RecyclerAdapter.class.getSimpleName();

    protected final LayoutInflater mInflater;
    protected final Timer timer = new Timer(new Handler());
    protected final Context mContext;

    protected final List<ListItem> mItems = new ArrayList<>();
    private final RecyclerView mListView;

    protected final CollapseExpandHandler collapseExpandHandler = new CollapseExpandHandler() {
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
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.setItem(mItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}

