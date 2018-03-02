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

    public void setTimeout(String id, Runnable runnable, long timeout) {
        Message message = Message.obtain(mHandler, runnable);
        message.obj = id;
        mHandler.sendMessageDelayed(message, timeout);
    }

    public void setSerializedInterval(String id, Runnable runnable, long interval) {
        setTimeout(
                id,
                () -> {
                    runnable.run();
                    setSerializedInterval(id, runnable, interval);
                },
                interval);
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
            setTimeout(clearId, () -> mLastRun.remove(id), millis);
            return;
        }
        setTimeout(id, runnable, millis);
    }
}
