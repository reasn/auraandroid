package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;


public class ColorButton extends AppCompatButton {

    public ColorButton(Context context) {
        super(context);
    }

    public ColorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    abstract private static class ColorSettableDrawable extends Drawable {
        abstract void setColors(@ColorInt int color, @ColorInt int accent);
    }

    private ColorSettableDrawable mColorButtonDrawable = new ColorSettableDrawable() {
        private static final int MARGIN = 30;
        private static final int BORDER = 5;
        private final Paint mColorPaint = new Paint();
        private final Paint mAccentPaint = new Paint();

        @Override
        public void draw(@NonNull Canvas canvas) {
            Paint p = new Paint();
            p.setColor(Color.BLACK);
            Rect b = getBounds();
            canvas.drawRect(new Rect(b.left + MARGIN, b.top + MARGIN, b.right - MARGIN, b.bottom - MARGIN), mAccentPaint);
            canvas.drawRect(new Rect(b.left + MARGIN + BORDER, b.top + MARGIN + BORDER, b.right - MARGIN - BORDER, b.bottom - MARGIN - BORDER), mColorPaint);
        }

        void setColors(@ColorInt int color, @ColorInt int accent) {
            mColorPaint.setColor(color);
            mAccentPaint.setColor(accent);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        protected boolean onLevelChange(int level) {
            invalidateSelf();
            return true;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    };


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mColorButtonDrawable.setBounds(new Rect(0, 0, getMeasuredWidth(), getMeasuredHeight()));
        setBackground(mColorButtonDrawable);
    }

    void setColors(@ColorInt int color, @ColorInt int accent) {
        mColorButtonDrawable.setColors(color, accent);
        invalidate();
    }

    /**
     * Thanks http://www.gadgetsaint.com/tips/dynamic-square-rectangular-layouts-android/
     */
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(size, size);
    }
}
