package io.auraapp.auraandroid.main;

import io.auraapp.auraandroid.common.Slogan;

class ListItem {
    private Slogan mSlogan;
    private boolean mMine;
    private boolean mExpanded = false;

    ListItem(Slogan slogan, boolean mine) {
        mSlogan = slogan;
        mMine = mine;
    }

    Slogan getSlogan() {
        return mSlogan;
    }

    boolean isMine() {
        return mMine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListItem listItem = (ListItem) o;

        return mMine == listItem.mMine && (
                mSlogan != null
                        ? mSlogan.equals(listItem.mSlogan)
                        : listItem.mSlogan == null
        );
    }
}
