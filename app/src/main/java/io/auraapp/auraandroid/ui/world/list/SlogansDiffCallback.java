package io.auraapp.auraandroid.ui.world.list;

import android.support.v7.util.DiffUtil;

import java.util.List;

import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.lists.SpacerItem;

public class SlogansDiffCallback extends DiffUtil.Callback {
    private List<?> mNewItems;
    private List<?> mOldItems;

    public SlogansDiffCallback(List<?> oldItems, List<?> newItems) {
        mNewItems = newItems;
        mOldItems = oldItems;
    }

    @Override
    public int getOldListSize() {
        return mOldItems.size();
    }

    @Override
    public int getNewListSize() {
        return mNewItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Object oldItem = mOldItems.get(oldItemPosition);
        Object newItem = mNewItems.get(newItemPosition);
        if (oldItem instanceof SpacerItem && newItem instanceof SpacerItem) {
            return true;
        }
        if (oldItem instanceof Slogan && newItem instanceof Slogan) {
            return ((Slogan) oldItem).isTheSameAs((Slogan) newItem);
        }
        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldItems.get(oldItemPosition).equals(mNewItems.get(newItemPosition));
    }
}
