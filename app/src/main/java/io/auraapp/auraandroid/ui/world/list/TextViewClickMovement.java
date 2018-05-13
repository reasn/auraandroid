package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * Thanks https://stackoverflow.com/questions/12418279/android-textview-with-clickable-links-how-to-capture-clicks/19989677
 */
public class TextViewClickMovement extends LinkMovementMethod {

    private final String TAG = TextViewClickMovement.class.getSimpleName();

    private final LinkTouchListener mListener;
    private final GestureDetector mGestureDetector;
    private TextView mWidget;
    private Spannable mBuffer;

    public enum LinkType {

        /**
         * Indicates that phone link was clicked
         */
        PHONE,

        /**
         * Identifies that URL was clicked
         */
        WEB_URL,

        /**
         * Identifies that Email Address was clicked
         */
        EMAIL_ADDRESS,

        /**
         * Indicates that none of above mentioned were clicked
         */
        NONE
    }

    /**
     * Interface used to handle Long clicks on the {@link TextView} and taps
     * on the phone, web, mail links inside of {@link TextView}.
     */
    public interface LinkTouchListener {

        void onLinkClicked(final String linkText);

        void onDown(final TextView textView);

        void onUp(final TextView textView);
    }


    public TextViewClickMovement(final LinkTouchListener listener, final Context context) {
        mListener = listener;
        mGestureDetector = new GestureDetector(context, new SimpleOnGestureListener());
    }

    @Override
    public boolean onTouchEvent(final TextView widget, final Spannable buffer, final MotionEvent event) {

        mWidget = widget;
        mBuffer = buffer;

        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {
            mListener.onDown(widget);
        } else {
            mListener.onUp(widget);
        }

        mGestureDetector.onTouchEvent(event);

        return false;
    }

    /**
     * Detects various gestures and events.
     * Notify users when a particular motion event has occurred.
     */
    class SimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            // Notified when a tap occurs.
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            // Notified when tap occurs.
            final String linkText = getLinkText(mWidget, mBuffer, event);

//            LinkType linkType = LinkType.NONE;
//
//            if (Patterns.PHONE.matcher(linkText).matches()) {
//                linkType = LinkType.PHONE;
//            } else if (Patterns.WEB_URL.matcher(linkText).matches()) {
//                linkType = LinkType.WEB_URL;
//            } else if (Patterns.EMAIL_ADDRESS.matcher(linkText).matches()) {
//                linkType = LinkType.EMAIL_ADDRESS;
//            }

//            if (mListener != null) {

                mListener.onLinkClicked(linkText);
//            }

            return false;
        }

        private String getLinkText(final TextView widget, final Spannable buffer, final MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

            if (link.length != 0) {
                return buffer.subSequence(buffer.getSpanStart(link[0]),
                        buffer.getSpanEnd(link[0])).toString();
            }

            return "";
        }
    }
}
