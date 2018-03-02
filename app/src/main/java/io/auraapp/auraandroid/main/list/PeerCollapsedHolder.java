package io.auraapp.auraandroid.main.list;

import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;

class PeerCollapsedHolder extends ItemViewHolder {

    private final TextView mSloganTextView;

    PeerCollapsedHolder(View itemView, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);

        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        itemView.setOnClickListener((v) -> {
            if (mItem != null) {
                collapseExpandHandler.flip(mItem);
            }
        });
    }

    void bind(ListItem item) {
        if (!(item instanceof PeerSloganListItem)) {
            throw new RuntimeException("Expecting " + PeerSloganListItem.class.getSimpleName());
        }
        mSloganTextView.setText(((PeerSloganListItem) item).getSlogan().getText());
    }
}
