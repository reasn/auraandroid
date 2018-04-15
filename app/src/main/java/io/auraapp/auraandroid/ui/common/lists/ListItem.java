package io.auraapp.auraandroid.ui.common.lists;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListItem listItem = (ListItem) o;

        if (mExpanded != listItem.mExpanded) return false;
        return mIndex != null ? mIndex.equals(listItem.mIndex) : listItem.mIndex == null;
    }

    @Override
    public int hashCode() {
        int result = (mExpanded ? 1 : 0);
        result = 31 * result + (mIndex != null ? mIndex.hashCode() : 0);
        return result;
    }
}
