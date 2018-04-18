package io.auraapp.auraandroid.ui.world.list;

import android.support.v7.util.DiffUtil;

import java.util.List;

import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.profile.SpacerItem;

public class PeersDiffCallback extends DiffUtil.Callback {
    List<ListItem> mNewItems;
    List<ListItem> mOldItems;

    public PeersDiffCallback(List<ListItem> oldItems, List<ListItem> newItems) {
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
        ListItem oldItem = mOldItems.get(oldItemPosition);
        ListItem newItem = mNewItems.get(newItemPosition);
        if (oldItem instanceof SpacerItem && newItem instanceof SpacerItem) {
            return true;
        }
        if (oldItem instanceof PeerItem && newItem instanceof PeerItem) {
            return ((PeerItem) oldItem).getPeer().mId == ((PeerItem) newItem).getPeer().mId;
        }
        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldItems.get(oldItemPosition).equals(mNewItems.get(newItemPosition));
    }
}
