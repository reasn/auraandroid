package io.auraapp.auraandroid.main;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.main.list.ListItem;
import io.auraapp.auraandroid.main.list.RecycleAdapter;

class RecycleViewHolder extends RecyclerView.ViewHolder {
    final TextView mSloganTextView;
    ListItem mItem;

    RecycleViewHolder(View itemView, RecycleAdapter.OnClickHandler mOnClickHandler) {
        super(itemView);
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        itemView.setOnClickListener((v) -> {
            mOnClickHandler.onClick(mItem);
        });
    }
}
