package io.auraapp.auraandroid.main.list.item;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Random;

public abstract class ItemViewHolder extends RecyclerView.ViewHolder {
    private final View mItemView;
    public ListItem mItem;

    public ItemViewHolder(View itemView) {
        super(itemView);
        mItemView = itemView;
    }

    public abstract void bind(ListItem item);

    public final void setItem(ListItem listItem) {
        mItem = listItem;
        bind(listItem);
        Random rnd = new Random();
        int color = Color.argb(
                255,
                rnd.nextInt(100) + 156,
                rnd.nextInt(100) + 156,
                rnd.nextInt(100) + 156);
        mItemView.setBackgroundColor(color);
    }
}
