package io.auraapp.auraandroid.ui.common.lists;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class ExpandableViewHolder extends RecyclerView.ViewHolder {

    protected ExpandableViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void bind(Object item, boolean expanded, View.OnClickListener collapseExpandHandler);
}
