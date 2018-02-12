package io.auraapp.auraandroid.main.list;

import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;

class MyExpandedHolder extends ItemViewHolder {

    private final TextView mSloganTextView;

    MyExpandedHolder(View itemView,
                     RecycleAdapter.CollapseExpandHandler collapseExpandHandler,
                     RecycleAdapter.OnEditHandler onEditHandler,
                     RecycleAdapter.OnDropHandler onDropHandler
    ) {
        super(itemView);

        mSloganTextView = itemView.findViewById(R.id.slogan_text);

        itemView.setOnClickListener((v) -> {
            collapseExpandHandler.flip(mItem);
        });
        itemView.findViewById(R.id.edit_button).setOnClickListener((View $) -> {
            onEditHandler.onEdit(mItem.getSlogan());
        });
        itemView.findViewById(R.id.drop_button).setOnClickListener((View $) -> {
            onDropHandler.onDrop(mItem.getSlogan());
        });
    }

    @Override
    void bind(ListItem item) {
        mSloganTextView.setText(item.getSlogan().getText());
    }
}