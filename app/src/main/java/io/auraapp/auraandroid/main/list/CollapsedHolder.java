package io.auraapp.auraandroid.main.list;

import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;

 class CollapsedHolder extends ItemViewHolder {

    private final TextView mSloganTextView;

    CollapsedHolder(View itemView, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);

        mSloganTextView = itemView.findViewById(R.id.slogan_text);

        itemView.setOnClickListener((v) -> collapseExpandHandler.flip(mItem));
    }

    void bind(ListItem item) {
        mSloganTextView.setText(item.getSlogan().getText());
    }
}
