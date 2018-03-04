package io.auraapp.auraandroid.main.list.item;

import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.main.list.RecycleAdapter;

public class PeerCollapsedHolder extends ItemViewHolder {

    private final TextView mSloganTextView;

    public PeerCollapsedHolder(View itemView, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);

        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        itemView.setOnClickListener($ -> collapseExpandHandler.flip(getLastBoundItem()));
    }

    public void bind(ListItem item, View itemView) {
        if (!(item instanceof PeerSloganListItem)) {
            throw new RuntimeException("Expecting " + PeerSloganListItem.class.getSimpleName());
        }
        mSloganTextView.setText(((PeerSloganListItem) item).getSlogan().getText());
    }
}
