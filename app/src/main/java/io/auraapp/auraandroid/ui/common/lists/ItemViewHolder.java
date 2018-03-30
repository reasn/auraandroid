package io.auraapp.auraandroid.ui.common.lists;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class ItemViewHolder extends RecyclerView.ViewHolder {
    private final View mItemView;
    private ListItem mItem;

    public ItemViewHolder(View itemView) {
        super(itemView);
        mItemView = itemView;
    }

    public abstract void bind(ListItem item, View mItemView);

    public final void setItem(ListItem listItem) {
        mItem = listItem;
        bind(mItem, mItemView);
    }

    private void hide() {

    }
    private void show() {

    }

    public ListItem getLastBoundItem() {
        return mItem;
    }
}
