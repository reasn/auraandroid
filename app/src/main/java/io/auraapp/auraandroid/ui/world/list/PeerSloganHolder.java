package io.auraapp.auraandroid.ui.world.list;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.util.Patterns;
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

    @FunctionalInterface
    public interface WhatsMyColorCallback {
        @ColorInt
        int getColor();
    }
    private static final String TAG = "aura/list/" + PeerSloganHolder.class.getSimpleName();
    private final WhatsMyColorCallback mWhatsMyColorCallback;
    private final Context mContext;
    private final OnAdoptCallback mOnAdoptCallback;
    private final TextView mTextView;

    @ColorInt
    int mTextColor;
    @ColorInt
    int mBackgroundColor;

    private final TextViewClickMovement.LinkTouchListener mLinkTouchListener = new TextViewClickMovement.LinkTouchListener() {

        @Override
        public void onLinkClicked(String linkText) {

            if (Patterns.IP_ADDRESS.matcher(linkText).matches()) {
                return;

            } else if (Patterns.PHONE.matcher(linkText).matches()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + linkText));
                mContext.startActivity(intent);

            } else if (Patterns.WEB_URL.matcher(linkText).matches()) {
                if (!linkText.startsWith("https://") && !linkText.startsWith("http://")) {
                    linkText = "http://" + linkText;
                }
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(linkText)));

            } else if (Patterns.EMAIL_ADDRESS.matcher(linkText).matches()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + linkText));
                mContext.startActivity(intent);
            }
        }

        @Override
        public void onDown(TextView textView) {
            startOrContinue((LinearLayout) textView.getParent());
        }

        @Override
        public void onUp(TextView textView) {
            stop((LinearLayout) textView.getParent());
        }
    };

    public PeerSloganHolder(View itemView, Context context, OnAdoptCallback onAdoptCallback, WhatsMyColorCallback whatsMyColorCallback) {
        super(itemView);
        mContext = context;
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
        mTextView.setMovementMethod(new TextViewClickMovement(mLinkTouchListener, mContext));

        itemView.setBackgroundColor(mBackgroundColor);
        mTextView.setTextColor(mTextColor);
        mTextView.setLinkTextColor(mTextColor);

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