package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapter;

public class MySloganHolder extends ItemViewHolder {

    private final TextView mSloganTextView;
    private final LinearLayout mExpandedWrapper;
    private final Button mEditButtonView;
    private final Button mDropButtonView;
    private final MySlogansRecycleAdapter.OnMySloganActionCallback mOnMySloganActionCallback;
    private Context mContext;

    public MySloganHolder(View itemView,
                          Context context,
                          MySlogansRecycleAdapter.OnMySloganActionCallback onMySloganActionCallback,
                          RecyclerAdapter.CollapseExpandHandler collapseExpandHandler) {
        super(itemView);
        mContext = context;
        mOnMySloganActionCallback = onMySloganActionCallback;
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        mExpandedWrapper = itemView.findViewById(R.id.expanded_wrapper);
        mEditButtonView = itemView.findViewById(R.id.edit_button);
        mDropButtonView = itemView.findViewById(R.id.drop_button);
        itemView.setOnClickListener($ -> collapseExpandHandler.flip(getLastBoundItem()));

        mEditButtonView.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_profile_list_item_edit)));
        mDropButtonView.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_profile_list_item_drop)));
    }

    @Override
    public void bind(ListItem item, View itemView) {
        if (!(item instanceof MySloganListItem)) {
            throw new RuntimeException("Expecting " + MySloganListItem.class.getSimpleName());
        }

        mItemView.setBackgroundColor(mBackgroundColor);

        MySloganListItem castItem = (MySloganListItem) item;
        mSloganTextView.setTextColor(mTextColor);
        mSloganTextView.setText(castItem.getSlogan().getText());
        if (!castItem.mExpanded) {
            if (mExpandedWrapper.getVisibility() == View.VISIBLE) {
                mExpandedWrapper.setVisibility(View.GONE);
            }
            return;
        }
        mExpandedWrapper.setVisibility(View.VISIBLE);

        mEditButtonView.setOnClickListener(
                $ -> mOnMySloganActionCallback.onActionTaken(castItem.getSlogan(), MySlogansRecycleAdapter.OnMySloganActionCallback.ACTION_EDIT)
        );
        mDropButtonView.setOnClickListener(
                $ -> mOnMySloganActionCallback.onActionTaken(castItem.getSlogan(), MySlogansRecycleAdapter.OnMySloganActionCallback.ACTION_DROP)
        );
    }
}