package io.auraapp.auraandroid.ui.profile;

import android.support.annotation.NonNull;

import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.world.list.item.ListItem;

public class MySloganListItem extends ListItem {

    private final Slogan mSlogan;

    public MySloganListItem(@NonNull Slogan slogan) {
        super("my-slogan-" + slogan.getText());
        mSlogan = slogan;
    }

    public Slogan getSlogan() {
        return mSlogan;
    }

    @Override
    public void updateWith(ListItem newItem) {
    }
}
