package io.auraapp.auraandroid.ui.world.list.item;

public class MySlogansHeadingItem extends ListItem {

    public int mMySlogansCount;
    final Runnable mShowCreateSloganDialogCallback;

    public MySlogansHeadingItem(int mySlogansCount, Runnable showCreateSloganDialogCallback) {
        super("my-slogans-heading");
        mMySlogansCount = mySlogansCount;
        mShowCreateSloganDialogCallback = showCreateSloganDialogCallback;
    }

    @Override
    public void updateWith(ListItem newItem) {
    }
}
