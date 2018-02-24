package io.auraapp.auraandroid.main.list;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import io.auraapp.auraandroid.common.Slogan;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class SwipeCallback extends ItemTouchHelper.SimpleCallback {

    @FunctionalInterface
    public interface OnSwipedCallback {
        void onSwiped(Slogan slogan, int action);
    }

    public static int ACTION_ADOPT = 1;
    public static int ACTION_EDIT = 2;
    public static int ACTION_DROP = 3;

    private final OnSwipedCallback mOnSwipedCallback;

    private final Drawable mRightIcon;
    private final int mRightIconWidth;
    private final int mRightIconHeight;

    private final Drawable mLeftIcon;
    private final int mLeftIconWidth;
    private final int mLeftIconHeight;

    private final ColorDrawable mBackground;
    private final int mBackgroundColor;

    public SwipeCallback(Context context, OnSwipedCallback onSwipedCallback) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

        mOnSwipedCallback = onSwipedCallback;

        mRightIcon = ContextCompat.getDrawable(context, android.R.drawable.checkbox_on_background);
        mRightIconWidth = mRightIcon.getIntrinsicWidth();
        mRightIconHeight = mRightIcon.getIntrinsicHeight();

        mLeftIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_delete);
        mLeftIconWidth = mLeftIcon.getIntrinsicWidth();
        mLeftIconHeight = mLeftIcon.getIntrinsicHeight();

        mBackground = new ColorDrawable();
        mBackgroundColor = Color.parseColor("#f44336");
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        if (!(viewHolder instanceof ItemViewHolder)) {
            throw new RuntimeException("Dragged item must be instance of " + ItemViewHolder.class);
        }
        ListItem item = ((ItemViewHolder) viewHolder).mItem;
        if (item.isMine()) {
            if (direction == ItemTouchHelper.LEFT) {
                mOnSwipedCallback.onSwiped(item.getSlogan(), ACTION_EDIT);
            } else if (direction == ItemTouchHelper.RIGHT) {
                mOnSwipedCallback.onSwiped(item.getSlogan(), ACTION_DROP);
            }
        } else if (direction == ItemTouchHelper.LEFT) {
            mOnSwipedCallback.onSwiped(item.getSlogan(), ACTION_ADOPT);
        }
    }

    @Override
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        v("swipe", "onChildDraw");

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();


        // Draw the red delete background
        mBackground.setColor(mBackgroundColor);
        mBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());
        mBackground.draw(canvas);

        // Left Icon
        int leftIconTop = itemView.getTop() + (itemHeight - mRightIconHeight) / 2;
        int leftIconMargin = (itemHeight - mRightIconHeight) / 2;

        mLeftIcon.setBounds(
                leftIconMargin,
                leftIconTop,
                mLeftIconWidth,
                leftIconTop + mLeftIconHeight);
        mLeftIcon.draw(canvas);

        // Right Icon
        int iconTop = itemView.getTop() + (itemHeight - mRightIconHeight) / 2;
        int iconMargin = (itemHeight - mRightIconHeight) / 2;

        mRightIcon.setBounds(
                itemView.getRight() - iconMargin - mRightIconWidth,
                iconTop,
                itemView.getRight() - iconMargin,
                iconTop + mRightIconHeight);
        mRightIcon.draw(canvas);

        // The `dX / 4` reduces the maximum distance the item can be swiped
        super.onChildDraw(canvas, recyclerView, viewHolder, dX / 4, dY, actionState, isCurrentlyActive);
    }


    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }
}
