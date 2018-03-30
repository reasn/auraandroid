package io.auraapp.auraandroid.ui.common.lists;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Timer;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public abstract class RecyclerAdapterWithSpacer extends RecyclerView.Adapter<ItemViewHolder> {

    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private static final String TAG = "@aura/" + RecyclerAdapterWithSpacer.class.getSimpleName();

    private static final int TYPE_SPACER = 143;

    protected final LayoutInflater mInflater;
    protected final Timer timer = new Timer(new Handler());
    protected final Context mContext;

    protected final List<ListItem> mItems = new ArrayList<>();
    private final RecyclerView mListView;

    protected final CollapseExpandHandler collapseExpandHandler = new CollapseExpandHandler() {
        @Override
        public void flip(ListItem item) {
            if (item == null || item instanceof SpacerItem) {
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

    public RecyclerAdapterWithSpacer(@NonNull Context context, RecyclerView listView) {
        super();
        mContext = context;
        mListView = listView;
        mInflater = LayoutInflater.from(context);
        mItems.add(new SpacerItem());
    }


    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SPACER) {
            return new SpacerItemHolder(
                    mInflater.inflate(R.layout.common_list_item_spacer, parent, false)
            );
        }
        throw new RuntimeException("Unknown view type " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.setItem(mItems.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof SpacerItem) {
            return TYPE_SPACER;
        }
        throw new RuntimeException("Unknown item " + mItems.get(position).getClass().getSimpleName());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}

