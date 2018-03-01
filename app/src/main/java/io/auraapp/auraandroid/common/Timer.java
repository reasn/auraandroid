package io.auraapp.auraandroid.common;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Timer {

    private final Handler mHandler;

    public Timer(Handler handler) {
        this.mHandler = handler;
    }

    public void set(String id, Runnable runnable, long millis) {

        Message message = Message.obtain(mHandler, runnable);
        message.obj = id;
        mHandler.sendMessageDelayed(message, millis);
    }

    public void clear(@Nullable String id) {
        if (id != null) {
            mHandler.removeCallbacksAndMessages(id);
        }
    }


    private final Map<String, Long> mLastRun = new HashMap<>();

    public void debounce(String id, Runnable runnable, long millis) {
        clear(id);

        String clearId = "clear-debounce-" + id;
        clear(clearId);

        long now = System.currentTimeMillis();
        Long lastRun = mLastRun.get(id);
        if (lastRun == null || lastRun < now - millis) {
            mLastRun.put(id, now);
            runnable.run();
            set(clearId, () -> mLastRun.remove(id), millis);
            return;
        }
        set(id, runnable, millis);
    }
}
