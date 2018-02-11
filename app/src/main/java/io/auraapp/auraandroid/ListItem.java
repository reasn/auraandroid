package io.auraapp.auraandroid;

import io.auraapp.auraandroid.Communicator.Slogan;

public class ListItem {
    private Slogan mSlogan;
    private boolean mMine;

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
