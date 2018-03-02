package io.auraapp.auraandroid.main.list;

abstract public class AbstractListItem {
    private String mIndex;

    AbstractListItem(String index) {
        mIndex = index;
    }

    int compareIndex(AbstractListItem item) {
        return mIndex.compareTo(item.mIndex);
    }
}
