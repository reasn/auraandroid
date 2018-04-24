package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.MonoSpaceText;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;

public class MySloganHolder extends ExpandableViewHolder {

    private final MonoSpaceText mSloganTextView;
    private final LinearLayout mExpandedWrapper;
    private final Button mEditButtonView;
    private final Button mDropButtonView;
    private final MySlogansRecycleAdapter.OnMySloganActionCallback mOnMySloganActionCallback;
    @ColorInt
    int mBackgroundColor;
    @ColorInt
    int mTextColor;

    public MySloganHolder(View itemView,
                          Context context,
                          MySlogansRecycleAdapter.OnMySloganActionCallback onMySloganActionCallback) {
        super(itemView);
        mOnMySloganActionCallback = onMySloganActionCallback;
        mSloganTextView = itemView.findViewById(R.id.slogan_text);
        mExpandedWrapper = itemView.findViewById(R.id.expanded_wrapper);
        mEditButtonView = itemView.findViewById(R.id.edit_button);
        mDropButtonView = itemView.findViewById(R.id.drop_button);

        mEditButtonView.setText(EmojiHelper.replaceShortCode(context.getString(R.string.ui_profile_list_item_edit)));
        mDropButtonView.setText(EmojiHelper.replaceShortCode(context.getString(R.string.ui_profile_list_item_drop)));
    }


    @Override
    public void bind(Object item, boolean expanded, View.OnClickListener collapseExpandHandler) {

        if (!(item instanceof Slogan)) {
            throw new RuntimeException("Expecting " + Slogan.class.getSimpleName());
        }

        itemView.setBackgroundColor(mBackgroundColor);
        itemView.setOnClickListener(collapseExpandHandler);

        Slogan slogan = (Slogan) item;

        mSloganTextView.setTextColor(mTextColor);
        mSloganTextView.setText(slogan.getText());
        mSloganTextView.setOnClickListener(collapseExpandHandler);

        if (!expanded) {
            if (mExpandedWrapper.getVisibility() == View.VISIBLE) {
                mExpandedWrapper.setVisibility(View.GONE);
            }
            return;
        }
        mExpandedWrapper.setVisibility(View.VISIBLE);

        mEditButtonView.setOnClickListener($ -> {
            mOnMySloganActionCallback.onActionTaken(slogan, MySlogansRecycleAdapter.OnMySloganActionCallback.ACTION_EDIT);
            collapseExpandHandler.onClick(itemView);
        });
        mDropButtonView.setOnClickListener($ -> {
            mOnMySloganActionCallback.onActionTaken(slogan, MySlogansRecycleAdapter.OnMySloganActionCallback.ACTION_DROP);
            collapseExpandHandler.onClick(itemView);
        });
    }
}