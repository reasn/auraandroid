package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.common.MonoSpaceText;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapter;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PeerSloganHolder extends ItemViewHolder {

    private static final String TAG = "aura/list/" + PeerSloganHolder.class.getSimpleName();
    private final Context mContext;
    private final OnAdoptCallback mOnAdoptCallback;
    private final MonoSpaceText mTextView;
    private final Button mAdoptButton;
    private final LinearLayout mExpandedWrapper;
    private final RecyclerAdapter.CollapseExpandHandler mCollapseExpandHandler;

    public PeerSloganHolder(View itemView,
                            Context context,
                            RecyclerAdapter.CollapseExpandHandler collapseExpandHandler,
                            OnAdoptCallback onAdoptCallback) {
        super(itemView);
        mContext = context;
        mOnAdoptCallback = onAdoptCallback;

        mTextView = itemView.findViewById(R.id.world_peer_slogan_text);
        mExpandedWrapper = itemView.findViewById(R.id.world_peer_slogan_expanded_wrapper);
        mAdoptButton = itemView.findViewById(R.id.world_peer_slogan_adopt_button);
        mCollapseExpandHandler = collapseExpandHandler;
    }

    @Override
    public void bind(ListItem item, View itemView) {
        v(TAG, "Binding peer slogan item view, expanded: %s", item.mExpanded);
        PeerSloganItem castItem = (PeerSloganItem) item;
        mTextView.setText(castItem.getSlogan().getText());
        mTextView.setOnClickListener($ -> mCollapseExpandHandler.flip(item));

        if (!item.mExpanded) {
            mExpandedWrapper.setVisibility(View.GONE);
            return;
        }
        mExpandedWrapper.setVisibility(View.VISIBLE);
        mAdoptButton.setOnClickListener($ -> mOnAdoptCallback.onAdoptIntended(castItem.getSlogan()));
    }
}