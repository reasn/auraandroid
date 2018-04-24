package io.auraapp.auraandroid.ui.world.list;

import android.support.annotation.ColorInt;
import android.view.View;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.MonoSpaceText;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PeerSloganHolder extends ExpandableViewHolder {

    private static final String TAG = "aura/list/" + PeerSloganHolder.class.getSimpleName();
    private final OnAdoptCallback mOnAdoptCallback;
    private final MonoSpaceText mTextView;

    @ColorInt
    public int mTextColor;
    @ColorInt
    public int mBackgroundColor;

    public PeerSloganHolder(View itemView, OnAdoptCallback onAdoptCallback) {
        super(itemView);
        mOnAdoptCallback = onAdoptCallback;
        mTextView = itemView.findViewById(R.id.world_peer_slogan_text);
    }

    @Override
    public void bind(Object item, boolean expanded, View.OnClickListener collapseExpandHandler) {
        v(TAG, "Binding peer slogan item view, expanded: %s", expanded);
        Slogan slogan = (Slogan) item;
        mTextView.setText(slogan.getText());
        mTextView.setOnClickListener(collapseExpandHandler);
        itemView.setBackgroundColor(mBackgroundColor);
        mTextView.setTextColor(mTextColor);

        itemView.setOnLongClickListener($ -> {
            mOnAdoptCallback.onAdoptIntended(slogan);
            return true;
        });
    }
}