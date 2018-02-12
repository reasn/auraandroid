package io.auraapp.auraandroid.main.list;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class ItemViewHolder extends RecyclerView.ViewHolder {
    protected ListItem mItem;

    ItemViewHolder(View itemView) {
        super(itemView);
    }

    abstract void bind(ListItem item);

    public final void setItem(ListItem listItem) {
        mItem = listItem;
        bind(listItem);
    }
}
