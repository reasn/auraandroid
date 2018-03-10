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

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.main.list.item.ItemViewHolder;
import io.auraapp.auraandroid.main.list.item.ListItem;
import io.auraapp.auraandroid.main.list.item.MyCollapsedHolder;
import io.auraapp.auraandroid.main.list.item.MyExpandedHolder;
import io.auraapp.auraandroid.main.list.item.MySloganListItem;
import io.auraapp.auraandroid.main.list.item.PeerCollapsedHolder;
import io.auraapp.auraandroid.main.list.item.PeerExpandedHolder;
import io.auraapp.auraandroid.main.list.item.PeerSloganListItem;

public class SwipeCallback extends ItemTouchHelper.SimpleCallback {

    @FunctionalInterface
    public interface OnSwipedCallback {
        void onSwiped(Slogan slogan, int action);
    }

    @FunctionalInterface
    public interface ResetItemViewCallback {
        void resetItemView();
    }

    public static final int ACTION_ADOPT = 1;
    public static final int ACTION_EDIT = 2;
    public static final int ACTION_DROP = 3;

    private final OnSwipedCallback mOnSwipedCallback;
    private final ResetItemViewCallback mResetItemViewCallback;

    private final Drawable mEditIcon;
    private final Drawable mDropIcon;
    private final Drawable mAdoptIcon;

    private final ColorDrawable mBackground;

    public SwipeCallback(Context context, ResetItemViewCallback resetItemViewCallback, OnSwipedCallback onSwipedCallback) {
        super(0, 0);

        mResetItemViewCallback = resetItemViewCallback;
        mOnSwipedCallback = onSwipedCallback;

        mEditIcon = ContextCompat.getDrawable(context, R.mipmap.ic_memo);
        mDropIcon = ContextCompat.getDrawable(context, R.mipmap.ic_wastebasket);
        mAdoptIcon = ContextCompat.getDrawable(context, R.mipmap.ic_fire);

        mBackground = new ColorDrawable();
        mBackground.setColor(Color.parseColor("#000000"));
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
            } else if (direction == ItemTouchHelper.RIGHT) {
                mOnSwipedCallback.onSwiped(castItem.getSlogan(), ACTION_DROP);
            }
        } else if (item instanceof PeerSloganListItem) {
            PeerSloganListItem castItem = (PeerSloganListItem) item;
            if (direction == ItemTouchHelper.LEFT) {
                mOnSwipedCallback.onSwiped(castItem.getSlogan(), ACTION_ADOPT);
            }
        }
        mResetItemViewCallback.resetItemView();
    }

    private void draw(Canvas canvas, Drawable icon, View itemView, boolean isLeft, int xxMargin) {

        int itemHeight = itemView.getBottom() - itemView.getTop();

        int height = Math.min(180, itemHeight / 2);
        int width = height * icon.getIntrinsicHeight() / icon.getIntrinsicWidth();

        int top = itemView.getTop() + (itemHeight - height) / 2;

        int xMargin = height / icon.getIntrinsicHeight() * xxMargin + (width > 50 ? 20 : 0);

        if (isLeft) {
            icon.setBounds(xMargin, top, xMargin + width, top + height);
        } else {
            icon.setBounds(itemView.getRight() - width - xMargin, top, itemView.getRight() - xMargin, top + height);
        }
        icon.draw(canvas);
    }

    @Override
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;

        mBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());
        mBackground.draw(canvas);

        if (viewHolder instanceof MyCollapsedHolder || viewHolder instanceof MyExpandedHolder) {
            draw(canvas, mDropIcon, itemView, true, -20);
            draw(canvas, mEditIcon, itemView, false, 0);
        } else {
            draw(canvas, mAdoptIcon, itemView, false, -20);
        }


        // The `dX / ?` reduces the maximum distance the item can be swiped
        super.onChildDraw(canvas, recyclerView, viewHolder, dX / 3, dY, actionState, isCurrentlyActive);
    }


    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }
}
