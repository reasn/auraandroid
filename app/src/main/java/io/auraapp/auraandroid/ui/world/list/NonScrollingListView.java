package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Thanks Hai Rom, https://stackoverflow.com/questions/18411494/android-listview-show-only-one-item
 */
public class NonScrollingListView extends ListView {

    public NonScrollingListView(Context context) {
        super(context);
    }
    public NonScrollingListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public NonScrollingListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int adjustedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, adjustedHeightMeasureSpec);
        getLayoutParams().height = getMeasuredHeight();
    }
}