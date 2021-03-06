package io.auraapp.auraandroid.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;

public class InfoBox extends LinearLayout {
    private TextView mEmojiView;
    private TextView mHeadingView;
    private TextView mTextView;
    private Button mButtonView;
    private TextView mSecondTextView;
    private final List<Runnable> mAttributeSetters = new ArrayList<>();

    public InfoBox(Context context) {
        super(context);
        init(context, null);
    }

    public InfoBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public InfoBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public InfoBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attributeSet) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.common_view_info_box, this);
        if (attributeSet != null) {
            TypedArray attrs = context.obtainStyledAttributes(attributeSet, R.styleable.InfoBox);
            mAttributeSetters.add(() -> {
                setHeading(attrs.getString(R.styleable.InfoBox_heading));
                setEmoji(attrs.getString(R.styleable.InfoBox_emoji));
                setText(attrs.getString(R.styleable.InfoBox_text));
                if (attrs.hasValue(R.styleable.InfoBox_button_caption)) {
                    mButtonView.setText(EmojiHelper.replaceShortCode(attrs.getString(R.styleable.InfoBox_button_caption)));
                    mButtonView.setVisibility(View.VISIBLE);
                }
                if (attrs.hasValue(R.styleable.InfoBox_second_text)) {
                    mSecondTextView.setText(EmojiHelper.replaceShortCode(attrs.getString(R.styleable.InfoBox_second_text)));
                    mSecondTextView.setVisibility(View.VISIBLE);
                }
                attrs.recycle();
            });
        }
    }

    public void hideButton() {
        mButtonView.setVisibility(View.GONE);
        mSecondTextView.setVisibility(View.GONE);
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
        mHeadingView = this.findViewById(R.id.dialog_edit_color_heading);
        mTextView = this.findViewById(R.id.text);
        mButtonView = this.findViewById(R.id.button);
        mSecondTextView = this.findViewById(R.id.second_text);
        for (Runnable r : mAttributeSetters) {
            r.run();
        }
        mAttributeSetters.clear();
    }

    public void setColor(@ColorRes int color) {
        setBackgroundColor(getContext().getResources().getColor(color));
    }
}
