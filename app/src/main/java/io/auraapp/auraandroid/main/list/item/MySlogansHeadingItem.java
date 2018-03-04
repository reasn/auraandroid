package io.auraapp.auraandroid.main.list.item;

public class MySlogansHeadingItem extends ListItem {

    public int mMySlogansCount;

    public MySlogansHeadingItem(int mySlogansCount) {
        super("my-slogans--heading");
        mMySlogansCount = mySlogansCount;
    }

    @Override
    public void updateWith(ListItem newItem) {
    }
}
