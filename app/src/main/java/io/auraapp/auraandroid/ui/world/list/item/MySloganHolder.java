package io.auraapp.auraandroid.ui.world.list.item;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.world.list.RecycleAdapter;

public class MySloganHolder extends ItemViewHolder {

    private final TextView mSloganTextView;
    private final boolean mExpanded;
    private final LinearLayout mExpandedWrapper;

    public MySloganHolder(View itemView, boolean expanded, RecycleAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);
        mExpanded = expanded;
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        mExpandedWrapper = itemView.findViewById(R.id.expanded_wrapper);
        itemView.setOnClickListener($ -> collapseExpandHandler.flip(getLastBoundItem()));
    }

    @Override
    public void bind(ListItem item, View itemView) {
        if (!(item instanceof MySloganListItem)) {
            throw new RuntimeException("Expecting " + MySloganListItem.class.getSimpleName());
        }
        MySloganListItem castItem = (MySloganListItem) item;
        mSloganTextView.setText(castItem.getSlogan().getText());
        if (!mExpanded) {
            mExpandedWrapper.setVisibility(View.GONE);
            return;
        }
        mExpandedWrapper.setVisibility(View.VISIBLE);
    }
}