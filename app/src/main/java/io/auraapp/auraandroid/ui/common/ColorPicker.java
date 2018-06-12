package io.auraapp.auraandroid.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import io.auraapp.auraandroid.R;

public class ColorPicker extends FrameLayout {

    public interface ColorListener {
        void onColorSelected(SelectedColor selectedColor);
    }

    public static class SelectedColor {

        private final float mPointX;
        private final float mPointY;
        private final String mColor;

        public SelectedColor(String color, float pointX, float pointY) {
            mColor = color;
            mPointX = pointX;
            mPointY = pointY;
        }

        public String getColor() {
            return this.mColor;
        }

        public float getPointX() {
            return mPointX;
        }

        public float getPointY() {
            return mPointY;
        }
    }

    private ImageView mPaletteView;
    private ImageView mSelectorView;
    private ColorListener mColorListener;

    public ColorPicker(Context context) {
        super(context);
    }

    public ColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void init(float x, float y) {

        mPaletteView = new ImageView(getContext());
        mPaletteView.setImageResource(R.drawable.profile_color_palette);
        mPaletteView.setScaleType(ImageView.ScaleType.MATRIX);
        mPaletteView.setAdjustViewBounds(true);

        LayoutParams paletteParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        paletteParams.gravity = Gravity.CENTER;
        addView(mPaletteView, paletteParams);

        mSelectorView = new ImageView(getContext());
        mSelectorView.setImageResource(R.drawable.profile_color_selector);

        int dimension = (int) getResources().getDimension(R.dimen.profile_color_selector_width);
        LayoutParams thumbParams = new LayoutParams(dimension, dimension);
        thumbParams.gravity = Gravity.CENTER;
        addView(mSelectorView, thumbParams);

        setSelector(new Point(
                (int) (x * (float) mPaletteView.getWidth()),
                (int) (y * (float) mPaletteView.getHeight())
        ));

        setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mSelectorView.setPressed(true);
                    return onTouchReceived(event);
                case MotionEvent.ACTION_MOVE:
                    mSelectorView.setPressed(true);
                    return onTouchReceived(event);
                case MotionEvent.ACTION_UP:
                    mSelectorView.setPressed(true);
                    return onTouchReceived(event);
                default:
                    mSelectorView.setPressed(false);
                    return false;
            }
        });
    }

    private boolean onTouchReceived(MotionEvent event) {
        Point snapPoint = new Point((int) event.getX(), (int) event.getY());
        int selectedColor = getColorFromBitmap(snapPoint.x, snapPoint.y);

        if (selectedColor == Color.TRANSPARENT) {
            return false;
        }
        mSelectorView.setX(snapPoint.x - (mSelectorView.getMeasuredWidth() / 2));
        mSelectorView.setY(snapPoint.y - (mSelectorView.getMeasuredHeight() / 2));

        triggerListener(snapPoint.x, snapPoint.y, selectedColor);

        return true;
    }

    private int getColorFromBitmap(float x, float y) {
        Matrix invertMatrix = new Matrix();
        mPaletteView.getImageMatrix().invert(invertMatrix);

        float[] mappedPoints = new float[]{x, y};
        invertMatrix.mapPoints(mappedPoints);

        if (mPaletteView.getDrawable() != null
                && mPaletteView.getDrawable() instanceof BitmapDrawable
                && mappedPoints[0] > 0 && mappedPoints[1] > 0
                && mappedPoints[0] < mPaletteView.getDrawable().getIntrinsicWidth()
                && mappedPoints[1] < mPaletteView.getDrawable().getIntrinsicHeight()) {

            invalidate();
            return ((BitmapDrawable) mPaletteView.getDrawable()).getBitmap().getPixel(
                    (int) mappedPoints[0],
                    (int) mappedPoints[1]
            );
        }
        return 0;
    }

    private void triggerListener(int x, int y, int color) {
        if (mColorListener != null) {
            mColorListener.onColorSelected(new SelectedColor(
                    "#" + String.format("%06X", (0xFFFFFF & color)).toLowerCase(),
                    (float) x / (float) mPaletteView.getWidth(),
                    (float) y / (float) mPaletteView.getHeight()
            ));
        }
    }

    public void setColorListener(ColorListener colorListener) {
        mColorListener = colorListener;
    }

    private void setSelector(Point point) {
        mSelectorView.setX(point.x - mSelectorView.getMeasuredWidth() / 2);
        mSelectorView.setY(point.y - mSelectorView.getMeasuredHeight() / 2);
        triggerListener(point.x, point.y, getColorFromBitmap(point.x, point.y));
    }
}
