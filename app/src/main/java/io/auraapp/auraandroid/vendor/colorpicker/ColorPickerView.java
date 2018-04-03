package io.auraapp.auraandroid.vendor.colorpicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import io.auraapp.auraandroid.R;

public class ColorPickerView extends FrameLayout {

    private int lastSelectedColor;
    private Point selectedPoint;

    private ImageView palette;
    private ImageView selector;

    protected ColorListener mColorListener;

    private boolean ACTON_UP = false;

    private String preferenceName;
    private ColorPickerSharedPreferencesManager sharedPreferencesManager;

    public ColorPickerView(Context context) {
        super(context);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        onCreate();
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        onCreate();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorPickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
        onCreate();
    }

    private void init() {
        sharedPreferencesManager = new ColorPickerSharedPreferencesManager(getContext());
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                onFirstLayout();
            }
        });
    }

    private void onFirstLayout() {
        if (getPreferenceName() != null) {
            int saved_x = sharedPreferencesManager.getInteger(getPreferenceName() + ColorPickerSharedPreferencesManager.POSITION_X, getMeasuredWidth() / 2);
            int saved_y = sharedPreferencesManager.getInteger(getPreferenceName() + ColorPickerSharedPreferencesManager.POSITION_Y, getMeasuredHeight() / 2);
            setSelectorPoint(saved_x, saved_y);
        } else
            selectCenter();
        fireColorListener();
        loadListeners();
    }

    private void onCreate() {
        setPadding(0, 0, 0, 0);
        palette = new ImageView(getContext());
        palette.setImageResource(R.drawable.color_spectrum);

        LayoutParams wheelParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        wheelParams.gravity = Gravity.CENTER;
        addView(palette, wheelParams);

        selector = new ImageView(getContext());
        selector.setImageResource(R.drawable.wheel);

        LayoutParams thumbParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        thumbParams.gravity = Gravity.CENTER;
        addView(selector, thumbParams);
    }

    private void loadListeners() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        selector.setPressed(true);
                        return onTouchReceived(event);
                    case MotionEvent.ACTION_MOVE:
                        selector.setPressed(true);
                        return onTouchReceived(event);
                    case MotionEvent.ACTION_UP:
                        selector.setPressed(true);
                        return onTouchReceived(event);
                    default:
                        selector.setPressed(false);
                        return false;
                }
            }
        });
    }

    private boolean onTouchReceived(MotionEvent event) {
        Point snapPoint = new Point((int) event.getX(), (int) event.getY());
        int selectedColor = getColorFromBitmap(snapPoint.x, snapPoint.y);

        if (selectedColor != Color.TRANSPARENT) {
            selector.setX(snapPoint.x - (selector.getMeasuredWidth() / 2));
            selector.setY(snapPoint.y - (selector.getMeasuredHeight() / 2));
            selectedPoint = new Point(snapPoint.x, snapPoint.y);
            lastSelectedColor = selectedColor;

            if (ACTON_UP) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    fireColorListener();
                }
            } else {
                fireColorListener();
            }

            return true;
        } else
            return false;
    }

    private int getColorFromBitmap(float x, float y) {
        Matrix invertMatrix = new Matrix();
        palette.getImageMatrix().invert(invertMatrix);

        float[] mappedPoints = new float[]{x, y};
        invertMatrix.mapPoints(mappedPoints);

        if (palette.getDrawable() != null && palette.getDrawable() instanceof BitmapDrawable &&
                mappedPoints[0] > 0 && mappedPoints[1] > 0 &&
                mappedPoints[0] < palette.getDrawable().getIntrinsicWidth() && mappedPoints[1] < palette.getDrawable().getIntrinsicHeight()) {

            invalidate();
            return ((BitmapDrawable) palette.getDrawable()).getBitmap().getPixel((int) mappedPoints[0], (int) mappedPoints[1]);
        }
        return 0;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    private void fireColorListener() {
        if (mColorListener != null) {
            mColorListener.onColorSelected(getColorEnvelope());
        }
    }

    public void setColorListener(ColorListener colorListener) {
        mColorListener = colorListener;
    }

    public void selectCenter() {
        setSelectorPoint(getMeasuredWidth() / 2, getMeasuredHeight() / 2);
    }

    public void setSelectorPoint(int x, int y) {
        selector.setX(x - getSelectorHalfWidth());
        selector.setY(y - getSelectorHalfHeight());
        selectedPoint = new Point(x, y);
        lastSelectedColor = getColorFromBitmap(x, y);
        fireColorListener();
    }

    public void setACTON_UP(boolean value) {
        this.ACTON_UP = value;
    }

    public void saveData() {
        if (getPreferenceName() != null) {
            sharedPreferencesManager.putInteger(getPreferenceName() + ColorPickerSharedPreferencesManager.POSITION_X, selectedPoint.x);
            sharedPreferencesManager.putInteger(getPreferenceName() + ColorPickerSharedPreferencesManager.POSITION_Y, selectedPoint.y);
            sharedPreferencesManager.putInteger(getPreferenceName() + ColorPickerSharedPreferencesManager.COLOR, lastSelectedColor);
        }
    }

    public void setPreferenceName(String preferenceName) {
        this.preferenceName = preferenceName;
    }

    public void setSavedColor(int color) {
        sharedPreferencesManager.clearSavedPositions(
                getPreferenceName() + ColorPickerSharedPreferencesManager.POSITION_X,
                getPreferenceName() + ColorPickerSharedPreferencesManager.POSITION_Y);
        sharedPreferencesManager.putInteger(getPreferenceName() + ColorPickerSharedPreferencesManager.COLOR, color);
    }

    public Point getSelectedPoint() {
        return selectedPoint;
    }

    private Point getCenterPoint(int x, int y) {
        return new Point(x - (selector.getMeasuredWidth() / 2), y - (selector.getMeasuredHeight() / 2));
    }

    public float getSelectorX() {
        return selector.getX() - getSelectorHalfWidth();
    }

    public float getSelectorY() {
        return selector.getY() - getSelectorHalfHeight();
    }

    public int getSelectorHalfWidth() {
        return selector.getMeasuredWidth() / 2;
    }

    public int getSelectorHalfHeight() {
        return selector.getMeasuredHeight() / 2;
    }

    public int getColor() {
        return lastSelectedColor;
    }

    public String getColorHtml() {
        return String.format("%06X", (0xFFFFFF & lastSelectedColor));
    }

    public int[] getColorRGB() {
        int[] rgb = new int[3];
        int color = (int) Long.parseLong(String.format("%06X", (0xFFFFFF & lastSelectedColor)), 16);
        rgb[0] = (color >> 16) & 0xFF; // hex to int : R
        rgb[1] = (color >> 8) & 0xFF; // hex to int : G
        rgb[2] = (color >> 0) & 0xFF; // hex to int : B
        return rgb;
    }

    public ColorEnvelope getColorEnvelope() {
        return new ColorEnvelope(getColor(), getColorHtml(), getColorRGB());
    }

    public String getPreferenceName() {
        return this.preferenceName;
    }

    public int getSavedColor(int defaultColor) {
        return sharedPreferencesManager.getInteger(getPreferenceName() + ColorPickerSharedPreferencesManager.COLOR, defaultColor);
    }

    public void clearSavedData() {
        sharedPreferencesManager.clearSavedData();
    }

    public ImageView getPaletteView() {
        return palette;
    }
}
