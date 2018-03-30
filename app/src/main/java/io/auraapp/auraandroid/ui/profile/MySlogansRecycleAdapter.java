package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.ListSynchronizer;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapterWithSpacer;
import io.auraapp.auraandroid.ui.common.lists.SpacerItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class MySlogansRecycleAdapter extends RecyclerAdapterWithSpacer {

    @FunctionalInterface
    public static interface OnMySloganActionCallback {
        int ACTION_EDIT = 2;
        int ACTION_DROP = 3;

        void onActionTaken(Slogan slogan, int action);
    }

    private static final String TAG = "@aura/" + MySlogansRecycleAdapter.class.getSimpleName();

    private final static int TYPE_MY_SLOGAN = 198;
    private final OnMySloganActionCallback mOnMySloganActionCallback;

    public MySlogansRecycleAdapter(@NonNull Context context,
                                   RecyclerView listView,
                                   OnMySloganActionCallback onMySloganActionCallback) {
        super(context, listView);
        mOnMySloganActionCallback = onMySloganActionCallback;
        mItems.add(new SpacerItem());
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
                existingItem -> existingItem instanceof SpacerItem,
                (item, newItem) -> item instanceof SpacerItem || item instanceof MySloganListItem || item.compareIndex(newItem) > 0
        );
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MY_SLOGAN) {
            return new MySloganHolder(
                    mInflater.inflate(R.layout.profile_list_item_slogan, parent, false),
                    mContext,
                    mOnMySloganActionCallback,
                    this.collapseExpandHandler
            );
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

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
        if (mItems.get(position) instanceof MySloganListItem) {
            return TYPE_MY_SLOGAN;
        }
        return super.getItemViewType(position);
    }
}

