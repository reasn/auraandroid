package io.auraapp.auraandroid.ui.world.list;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.support.annotation.ColorInt;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;

import static io.auraapp.auraandroid.common.FormattedLog.v;

public class PeerSloganHolder extends ExpandableViewHolder {

    private final WhatsMyColorCallback mWhatsMyColorCallback;

    public static interface WhatsMyColorCallback {
        @ColorInt
        public int getColor();
    }

    private static final String TAG = "aura/list/" + PeerSloganHolder.class.getSimpleName();
    private final OnAdoptCallback mOnAdoptCallback;
    private final TextView mTextView;

    @ColorInt
    public int mTextColor;
    @ColorInt
    public int mBackgroundColor;


    public PeerSloganHolder(View itemView, OnAdoptCallback onAdoptCallback, WhatsMyColorCallback whatsMyColorCallback) {
        super(itemView);
        mOnAdoptCallback = onAdoptCallback;
        mWhatsMyColorCallback = whatsMyColorCallback;
        mTextView = itemView.findViewById(R.id.world_peer_slogan_text);

        itemView.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {

                startOrContinue((LinearLayout) view);
                return true;
            }
            stop((LinearLayout) view);
            return false;
        });
    }

    @Override
    public void bind(Object item, boolean expanded, View.OnClickListener collapseExpandHandler) {
        v(TAG, "Binding peer slogan item view, expanded: %s", expanded);
        Slogan slogan = (Slogan) item;
        mTextView.setText(slogan.getText());
        itemView.setBackgroundColor(mBackgroundColor);
        mTextView.setTextColor(mTextColor);

        itemView.setTag(R.id.world_peer_slogan_tag_slogan, slogan);
    }

    /**
     * Thanks https://stackoverflow.com/questions/2614545/animate-change-of-view-background-color-on-android
     * Thanks https://stackoverflow.com/questions/2859574/the-key-must-be-an-application-specific-resource-id
     */
    private void stop(LinearLayout item) {
        Animator animator = (Animator) item.getTag(R.id.world_peer_slogan_tag_animator);
        if (animator != null) {
            animator.cancel();
        }
    }

    private void startOrContinue(LinearLayout item) {

        if (item.getTag(R.id.world_peer_slogan_tag_animator) != null) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), mBackgroundColor, mWhatsMyColorCallback.getColor());
        animator.setDuration(Config.WORLD_SLOGAN_ADOPT_PRESS_DURATION);
        // Not animating/changing text color because for brightnesses around 128 there will be flips
        animator.addUpdateListener(colorAnimator -> item.setBackgroundColor((int) colorAnimator.getAnimatedValue()));
        animator.start();
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                item.setTag(R.id.world_peer_slogan_tag_animator, null);
                item.setBackgroundColor(mBackgroundColor);

                Slogan slogan = (Slogan) item.getTag(R.id.world_peer_slogan_tag_slogan);
                if (slogan != null) {
                    mOnAdoptCallback.onAdoptIntended(slogan);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                item.setTag(R.id.world_peer_slogan_tag_animator, null);
                item.setBackgroundColor(mBackgroundColor);
                animation.removeAllListeners();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        item.setTag(R.id.world_peer_slogan_tag_animator, animator);
    }
}