package io.auraapp.auraandroid.ui.common.lists;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


abstract public class ExpandableRecyclerAdapter extends RecyclerView.Adapter<ExpandableViewHolder> {

    protected int mItemCountToExpandEverything = -1;
    protected final LayoutInflater mInflater;
    private RecyclerView mRecyclerView;

    public ExpandableRecyclerAdapter(Context context, RecyclerView recyclerView) {
        super();
        mRecyclerView = recyclerView;
        mInflater = LayoutInflater.from(context);
    }

    private int mExpandedIndex = -1;
    protected final List<Object> mItems = new ArrayList<>();

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ExpandableViewHolder holder, int position) {
        // Always expand if there's one item and one spacer item
        holder.bind(
                mItems.get(position),
                mItems.size() == mItemCountToExpandEverything || mExpandedIndex == position,
                createCollapseExpandHandler(holder));
    }

    private View.OnClickListener createCollapseExpandHandler(ExpandableViewHolder holder) {
        return $ -> {
            int position = holder.getAdapterPosition();
            // TODO add nice animations, https://gist.github.com/ZkHaider/9bf0e1d7b8a2736fd676
//            TransitionManager.beginDelayedTransition(mRecyclerView);

            if (mExpandedIndex == position) {
                mExpandedIndex = -1;
                notifyItemChanged(position);
            } else if (mExpandedIndex == -1) {
                mExpandedIndex = position;
                notifyItemChanged(position);
            } else {
                int previousExpandedIndex = mExpandedIndex;
                mExpandedIndex = position;
                notifyItemChanged(previousExpandedIndex);
                notifyItemChanged(position);
            }
        };
    }
}
