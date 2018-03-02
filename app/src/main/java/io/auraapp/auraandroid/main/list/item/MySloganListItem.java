package io.auraapp.auraandroid.main.list.item;

import android.support.annotation.NonNull;

import io.auraapp.auraandroid.common.Slogan;

public class MySloganListItem extends ListItem {

    private final Slogan mSlogan;

    public MySloganListItem(@NonNull Slogan slogan) {
        super(slogan.getText());
        mSlogan = slogan;
    }

    public Slogan getSlogan() {
        return mSlogan;
    }

    @Override
    public void updateWith(ListItem newItem) {
    }
}
