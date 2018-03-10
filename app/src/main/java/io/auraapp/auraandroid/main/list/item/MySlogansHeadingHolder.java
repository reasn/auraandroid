package io.auraapp.auraandroid.main.list.item;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.main.InfoBox;

public class MySlogansHeadingHolder extends ItemViewHolder {

    private final TextView mHeadingTextView;
    private Context mContext;
    private InfoBox mInfoBox;

    public MySlogansHeadingHolder(View itemView, Context context) {
        super(itemView);
        mContext = context;
        mHeadingTextView = itemView.findViewById(R.id.heading);
        mInfoBox = itemView.findViewById(R.id.info_box);
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
            mInfoBox.setVisibility(View.GONE);
            return;
        }

        mInfoBox.setEmoji(":eyes:");
        mInfoBox.setHeading(R.string.ui_main_my_slogans_info_no_slogans_heading);
        mInfoBox.setText(R.string.ui_main_my_slogans_info_no_slogans_text);
        mInfoBox.showButton(
                R.string.ui_main_my_slogans_info_no_slogans_heading_cta,
                0,
                $ -> {
                    // add slogan
                });
        mInfoBox.setColor(R.color.infoBoxNeutral);
        mInfoBox.setVisibility(View.VISIBLE);
    }
}
