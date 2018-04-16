package io.auraapp.auraandroid.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;

public class MonoSpaceText extends LinearLayout {
    private EditText mTextView;
    private final List<Runnable> mAttributeSetters = new ArrayList<>();

    public MonoSpaceText(Context context) {
        super(context);
        init(context, null);
    }

    public MonoSpaceText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MonoSpaceText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public MonoSpaceText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attributeSet) {
        LayoutInflater.from(context).inflate(R.layout.common_mono_space_text, this);
        if (attributeSet != null) {
            TypedArray attrs = context.obtainStyledAttributes(attributeSet, R.styleable.MonoSpaceText);
            mAttributeSetters.add(() -> {
                boolean editable = attrs.getBoolean(R.styleable.MonoSpaceText_editable, false);

                // EditTexts keep their state and might ignore setText without this setting
                mTextView.setSaveEnabled(editable);

                mTextView.setFocusable(editable);
                mTextView.setFocusableInTouchMode(editable);
                mTextView.setLongClickable(editable);
                mTextView.setCursorVisible(editable);

                mTextView.setMinLines(attrs.getInt(R.styleable.MonoSpaceText_minLines, 1));
                mTextView.setMaxLines(attrs.getInt(R.styleable.MonoSpaceText_maxLines, 20));
                int padding = attrs.getDimensionPixelSize(R.styleable.MonoSpaceText_textBoxPadding, 0);
                mTextView.setPadding(padding, padding, padding, padding);

                if (attrs.getBoolean(R.styleable.MonoSpaceText_alignCenter, false)) {
                    mTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                }
                if (attrs.getBoolean(R.styleable.MonoSpaceText_largeText, false)) {
                    mTextView.setTextAppearance(context, android.R.style.TextAppearance_Large);
                } else {
                    mTextView.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                }

                attrs.recycle();
            });
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        mTextView.setOnClickListener(l);
    }

    public void setText(String text) {
        mTextView.setText(text);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTextView = this.findViewById(R.id.common_mono_space_text_texst);
        for (Runnable r : mAttributeSetters) {
            r.run();
        }
        mAttributeSetters.clear();
    }

    public void setColor(@ColorRes int color) {
        setBackgroundColor(getContext().getResources().getColor(color));
    }

    public void setTextColor(int color) {
        mTextView.setTextColor(color);
    }

    public void setHint(String hint) {
        mTextView.setHint(hint);
    }

    public String getTextAsString() {
        return mTextView.getText().toString();
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        mTextView.addTextChangedListener(textWatcher);
    }

    public void setFilters(InputFilter[] inputFilters) {
        mTextView.setFilters(inputFilters);
    }

    public void setSelection(int i) {
        mTextView.setSelection(i);
    }
}
