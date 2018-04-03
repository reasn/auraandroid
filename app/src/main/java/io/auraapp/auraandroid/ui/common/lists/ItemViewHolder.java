package io.auraapp.auraandroid.ui.common.lists;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class ItemViewHolder extends RecyclerView.ViewHolder {
    protected final View mItemView;
    private ListItem mItem;
    protected int mTextColor = Color.BLACK;
    protected int mBackgroundColor = Color.WHITE;

    public ItemViewHolder(View itemView) {
        super(itemView);
        mItemView = itemView;
    }

    public abstract void bind(ListItem item, View mItemView);

    public final void setItem(ListItem listItem) {
        mItem = listItem;
        bind(mItem, mItemView);
    }

    public ListItem getLastBoundItem() {
        return mItem;
    }

    public void colorize(int backgroundColor, int textColor) {
        if (mBackgroundColor != backgroundColor || mTextColor != textColor) {
            mBackgroundColor = backgroundColor;
            mTextColor = textColor;
            bind(mItem, mItemView);
        }
    }
}
