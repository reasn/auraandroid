package io.auraapp.auraandroid.ui.world.list;

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
import io.auraapp.auraandroid.ui.world.list.item.ItemViewHolder;
import io.auraapp.auraandroid.ui.world.list.item.ListItem;
import io.auraapp.auraandroid.ui.world.list.item.MySloganHolder;
import io.auraapp.auraandroid.ui.world.list.item.MySloganListItem;
import io.auraapp.auraandroid.ui.world.list.item.PeersHeadingItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class MySlogansRecycleAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    @FunctionalInterface
    public interface CollapseExpandHandler {
        void flip(ListItem item);
    }

    private static final String TAG = "@aura/" + MySlogansRecycleAdapter.class.getSimpleName();

    private final static int TYPE_MY_SLOGAN_COLLAPSED = 144;
    private final static int TYPE_MY_SLOGAN_EXPANDED = 145;

    private final LayoutInflater mInflater;
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

    public MySlogansRecycleAdapter(@NonNull Context context, List<ListItem> items, RecyclerView listView) {
        super();
        mContext = context;
        mItems = items;
        mListView = listView;
        mInflater = LayoutInflater.from(context);
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
                existingItem -> existingItem instanceof MySloganListItem,
                existingItem -> existingItem instanceof PeersHeadingItem,
                (item, newItem) -> item instanceof PeersHeadingItem || item instanceof MySloganListItem || item.compareIndex(newItem) > 0
        );
    }

    public void notifyListItemChanged(ListItem item) {
        int index = mItems.indexOf(item);
        v(TAG, "notifyListItemChanged at %d, item: %s", index, item);
        notifyItemChanged(index);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MySloganHolder(
                mInflater.inflate(R.layout.list_item_my_slogan, parent, false),
                viewType == TYPE_MY_SLOGAN_EXPANDED
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.setItem(mItems.get(position));

        if (holder instanceof MySloganHolder) {
            // Alternating colors
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.yellow));
            } else {
                holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.dark_yellow));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        ListItem item = mItems.get(position);


        return item.mExpanded
                ? TYPE_MY_SLOGAN_EXPANDED
                : TYPE_MY_SLOGAN_COLLAPSED;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}

