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
import io.auraapp.auraandroid.main.list.item.ItemViewHolder;
import io.auraapp.auraandroid.main.list.item.ListItem;
import io.auraapp.auraandroid.main.list.item.MyCollapsedHolder;
import io.auraapp.auraandroid.main.list.item.MyExpandedHolder;
import io.auraapp.auraandroid.main.list.item.MySloganListItem;
import io.auraapp.auraandroid.main.list.item.PeerCollapsedHolder;
import io.auraapp.auraandroid.main.list.item.PeerExpandedHolder;
import io.auraapp.auraandroid.main.list.item.PeerSloganListItem;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class SwipeCallback extends ItemTouchHelper.SimpleCallback {


    @FunctionalInterface
    public interface OnSwipedCallback {
        void onSwiped(Slogan slogan, int action);
    }

    @FunctionalInterface
    public interface ResetItemViewCallback {
        void resetItemView(ListItem item);
    }

    public static final int ACTION_ADOPT = 1;
    public static final int ACTION_EDIT = 2;
    public static final int ACTION_DROP = 3;

    private final OnSwipedCallback mOnSwipedCallback;
    private final ResetItemViewCallback mResetItemViewCallback;

    private final Drawable mRightIcon;
    private final int mRightIconWidth;
    private final int mRightIconHeight;

    private final Drawable mLeftIcon;
    private final int mLeftIconWidth;
    private final int mLeftIconHeight;

    private final ColorDrawable mBackground;
    private final int mBackgroundColor;

    public SwipeCallback(Context context, ResetItemViewCallback resetItemViewCallback, OnSwipedCallback onSwipedCallback) {
        super(0, 0);

        mResetItemViewCallback = resetItemViewCallback;
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
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof PeerCollapsedHolder || viewHolder instanceof PeerExpandedHolder) {
            return ItemTouchHelper.LEFT;
        }
        if (viewHolder instanceof MyExpandedHolder || viewHolder instanceof MyCollapsedHolder) {
            return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        }
        return 0;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        if (!(viewHolder instanceof ItemViewHolder)) {
            throw new RuntimeException("Dragged item must be instance of " + ItemViewHolder.class);
        }
        ListItem item = ((ItemViewHolder) viewHolder).getLastBoundItem();
        if (item instanceof MySloganListItem) {
            MySloganListItem castItem = (MySloganListItem) item;
            if (direction == ItemTouchHelper.LEFT) {
                mOnSwipedCallback.onSwiped(castItem.getSlogan(), ACTION_EDIT);
                mResetItemViewCallback.resetItemView(item);
            } else if (direction == ItemTouchHelper.RIGHT) {
                mOnSwipedCallback.onSwiped(castItem.getSlogan(), ACTION_DROP);
                mResetItemViewCallback.resetItemView(item);
            }
        } else if (item instanceof PeerSloganListItem) {
            PeerSloganListItem castItem = (PeerSloganListItem) item;
            if (direction == ItemTouchHelper.LEFT) {
                mOnSwipedCallback.onSwiped(castItem.getSlogan(), ACTION_ADOPT);
                mResetItemViewCallback.resetItemView(item);
            }
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
        super.onChildDraw(canvas, recyclerView, viewHolder, dX / 5, dY, actionState, isCurrentlyActive);
    }


    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }
}
