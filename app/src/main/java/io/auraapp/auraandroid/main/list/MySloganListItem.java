package io.auraapp.auraandroid.main.list;

import android.support.annotation.NonNull;

import io.auraapp.auraandroid.common.Slogan;

class MySloganListItem extends ListItem {

    private final Slogan mSlogan;

    MySloganListItem(@NonNull Slogan slogan) {
        super(slogan.getText());
        mSlogan = slogan;
    }

    Slogan getSlogan() {
        return mSlogan;
    }

    @Override
    void updateWith(ListItem newItem) {
    }
}
