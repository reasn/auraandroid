package io.auraapp.auraandroid.ui.world;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.widget.Toast;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;

import static io.auraapp.auraandroid.common.Config.SWIPE_TO_REFRESH_DURATION;
import static io.auraapp.auraandroid.common.FormattedLog.i;


public class FakeSwipeRefreshLayout extends SwipeRefreshLayout {

    private static final String TAG = "@aura/ui/" + FakeSwipeRefreshLayout.class.getSimpleName();
    private final Handler mHandler = new Handler();

    private int mPeerCount = 0;

    public FakeSwipeRefreshLayout(@NonNull Context context) {
        super(context);
        setOnRefreshListener(this::refresh);
    }

    public FakeSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOnRefreshListener(this::refresh);
    }

    public void setPeerCount(int peerCount) {
        this.mPeerCount = peerCount;
    }

    /**
     * As the advertisement of peers are continuously monitored, triggering a refresh has zero impact.
     * As the user indicated a wish for an update, we briefly toast the current status.
     */
    void refresh() {
        i(TAG, "Showing swipe to refresh indicator to transport sense of immediacy");
        String text = getResources().getQuantityString(R.plurals.ui_world_toast_refresh, mPeerCount, mPeerCount);

        Toast.makeText(getContext(), EmojiHelper.replaceShortCode(text), Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(() -> setRefreshing(false), SWIPE_TO_REFRESH_DURATION);
    }
}
