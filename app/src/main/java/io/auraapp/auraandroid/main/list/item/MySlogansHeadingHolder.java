package io.auraapp.auraandroid.main.list.item;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;

public class MySlogansHeadingHolder extends ItemViewHolder {

    private final TextView mHeadingTextView;
    private Context mContext;
    private TextView mInfoTextView;

    public MySlogansHeadingHolder(View itemView, Context context) {
        super(itemView);
        mContext = context;
        mHeadingTextView = itemView.findViewById(R.id.heading);
        mInfoTextView = itemView.findViewById(R.id.info);
    }

    @Override
    public void bind(ListItem item, View mItemView) {
        if (!(item instanceof MySlogansHeadingItem)) {
            throw new RuntimeException("Expecting " + MySlogansHeadingItem.class.getSimpleName());
        }
        MySlogansHeadingItem castItem = ((MySlogansHeadingItem) item);

        mHeadingTextView.setText(EmojiHelper.replaceShortCode(mContext.getResources().getQuantityString(
                R.plurals.ui_main_my_slogans_heading, castItem.mMySlogansCount, castItem.mMySlogansCount
        )));
        if (castItem.mMySlogansCount > 0) {
            mInfoTextView.setVisibility(View.GONE);
        } else {
            mInfoTextView.setText(EmojiHelper.replaceShortCode(mContext.getString(R.string.ui_main_my_slogans_heading_no_slogans_text)));
            mInfoTextView.setVisibility(View.VISIBLE);
        }

    }
}
