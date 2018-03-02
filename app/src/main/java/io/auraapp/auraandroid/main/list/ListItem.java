package io.auraapp.auraandroid.main.list;

abstract public class ListItem {
    boolean mExpanded = false;
    private String mIndex;

    ListItem(String index) {
        mIndex = index;
    }

    int compareIndex(ListItem item) {
        return mIndex.compareTo(item.mIndex);
    }

    public String getIndex() {
        return mIndex;
    }

    abstract void updateWith(ListItem newItem);
}
