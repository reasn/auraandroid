package io.auraapp.auraandroid.main.list;

import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;

class MyCollapsedHolder extends ItemViewHolder {

    private final TextView mSloganTextView;

    MyCollapsedHolder(View itemView) {
        super(itemView);
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
    }

    void bind(ListItem item) {
        if (!(item instanceof MySloganListItem)) {
            throw new RuntimeException("Expecting " + MySloganListItem.class.getSimpleName());
        }
        mSloganTextView.setText(((MySloganListItem) item).getSlogan().getText());
    }
}
