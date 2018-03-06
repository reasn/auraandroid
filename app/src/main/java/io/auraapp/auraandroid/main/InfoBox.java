package io.auraapp.auraandroid.main;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;

public class InfoBox extends LinearLayout {
    private TextView mEmojiView;
    private TextView mHeadingView;
    private TextView mTextView;
    private Button mButtonView;
    private TextView mTextBelowButtonView;

    public InfoBox(Context context) {
        super(context);
        init(null);
    }

    public InfoBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public InfoBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public InfoBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_info_box, this);
    }

    public void showButton(@StringRes int caption, @StringRes int textBelowButton, OnClickListener onClickListener) {
        mButtonView.setText(EmojiHelper.replaceShortCode(getContext().getString(caption)));
        mButtonView.setVisibility(View.VISIBLE);
        mButtonView.setOnClickListener(onClickListener);
        mTextBelowButtonView.setText(getContext().getString(textBelowButton));
        mTextBelowButtonView.setVisibility(View.VISIBLE);
    }

    public void hideButton() {
        mButtonView.setVisibility(View.GONE);
        mTextBelowButtonView.setVisibility(View.GONE);
    }

    public void setHeading(String heading) {
        mHeadingView.setText(EmojiHelper.replaceShortCode(heading));
    }

    public void setEmoji(String emoji) {
        mEmojiView.setText(EmojiHelper.replaceShortCode(emoji));
    }

    public void setText(String text) {
        mTextView.setText(EmojiHelper.replaceShortCode(text));
    }

    public void setHeading(@StringRes int heading) {
        mHeadingView.setText(EmojiHelper.replaceShortCode(getContext().getString(heading)));
    }

    public void setText(@StringRes int text) {
        mTextView.setText(EmojiHelper.replaceShortCode(getContext().getString(text)));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mEmojiView = this.findViewById(R.id.emoji);
        mHeadingView = this.findViewById(R.id.heading);
        mTextView = this.findViewById(R.id.text);
        mButtonView = this.findViewById(R.id.button);
        mTextBelowButtonView = this.findViewById(R.id.text_below_button);
    }

    public void setColor(@ColorRes int color) {
        setBackgroundColor(getContext().getResources().getColor(color));
    }
}
