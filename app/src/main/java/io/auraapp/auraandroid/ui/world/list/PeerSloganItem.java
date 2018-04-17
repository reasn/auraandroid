package io.auraapp.auraandroid.ui.world.list;

import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.lists.ListItem;

public class PeerSloganItem extends ListItem {

    private Slogan mSlogan;

    public PeerSloganItem(String index, Slogan slogan) {
        super(index);
        mSlogan = slogan;
    }

    @Override
    public void updateWith(ListItem newItem) {
        mSlogan = ((PeerSloganItem) newItem).getSlogan();
    }

    public Slogan getSlogan() {
        return mSlogan;
    }
}
