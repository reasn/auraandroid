package io.auraapp.auraandroid.ui.world.list.item;

import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;

public class MyCollapsedHolder extends ItemViewHolder {

    private final TextView mSloganTextView;

    public MyCollapsedHolder(View itemView) {
        super(itemView);
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
    }

    public void bind(ListItem item, View itemView) {
        if (!(item instanceof MySloganListItem)) {
            throw new RuntimeException("Expecting " + MySloganListItem.class.getSimpleName());
        }
        mSloganTextView.setText(((MySloganListItem) item).getSlogan().getText());
    }
}
