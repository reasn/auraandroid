package io.auraapp.auraandroid.ui.world.list.item;

import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;

public class MyExpandedHolder extends ItemViewHolder {

    private final TextView mSloganTextView;

    public MyExpandedHolder(View itemView) {
        super(itemView);
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
    }

    @Override
    public void bind(ListItem item, View itemView) {
        if (!(item instanceof MySloganListItem)) {
            throw new RuntimeException("Expecting " + MySloganListItem.class.getSimpleName());
        }
        MySloganListItem castItem = (MySloganListItem) item;
        mSloganTextView.setText(castItem.getSlogan().getText());
    }
}