package io.auraapp.auraandroid.ui.world.list.item;

abstract public class ListItem {
    public boolean mExpanded = false;
    private String mIndex;

    public ListItem(String index) {
        mIndex = index;
    }

    public int compareIndex(ListItem item) {
        return mIndex.compareTo(item.mIndex);
    }

    public String getIndex() {
        return mIndex;
    }

    public abstract void updateWith(ListItem newItem);
}
