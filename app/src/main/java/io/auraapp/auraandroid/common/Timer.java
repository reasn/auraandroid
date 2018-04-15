package io.auraapp.auraandroid.common;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;

public class Timer {

    public static void clear(@Nullable Timeout timeout) {
        if (timeout != null) {
            timeout.clear();
        }
    }

    @FunctionalInterface
    public static interface Timeout {
        void clear();
    }

    private static int mTimerIndex = 0;

    private final Handler mHandler;
    private final String mIdPrefix;

    /**
     * Each Timer has its own prefix so that there's no collisions with
     * other Timer instances using the same handler
     */
    public Timer(Handler handler) {
        this.mHandler = handler;
        this.mIdPrefix = "timer-" + mTimerIndex + "-";
        mTimerIndex++;
    }

    public Timeout setTimeout(Runnable runnable, long timeout) {
        // Collisions don't matter because the queue implementation uses == instead of equals().
        // Math.random() keeps the compiler from optimizing -.-
        return setTimeout("timer-" + Math.random(), runnable, timeout);
    }

    private Timeout setTimeout(String token, Runnable runnable, long timeout) {
        Message message = Message.obtain(mHandler, runnable);
        message.obj = token;
        mHandler.sendMessageDelayed(message, timeout);
        return () -> mHandler.removeCallbacksAndMessages(token);
    }

    public Timeout setSerializedInterval(Runnable runnable, long interval) {
        return setSerializedInterval("timer-" + Math.random(), runnable, interval);
    }

    private Timeout setSerializedInterval(String token, Runnable runnable, long interval) {
        setTimeout(
                token,
                () -> {
                    runnable.run();
                    setSerializedInterval(token, runnable, interval);
                },
                interval);
        return () -> mHandler.removeCallbacksAndMessages(token);
    }

    public static class Debouncer {
        private final long mInterval;
        Timeout mRunTimeout;
        private long mLastRun = 0;
        private final Timer mTimer;

        public Debouncer(Timer timer, long interval) {
            this.mTimer = timer; mInterval = interval;
        }

        public void debounce(Runnable runnable) {
            Timer.clear(mRunTimeout);

            long now = System.currentTimeMillis();
            if (mLastRun < now - mInterval) {
                mLastRun = now;
                runnable.run();
                return;
            }
            mLastRun = now + mInterval;
            mRunTimeout = mTimer.setTimeout(runnable, mInterval);
        }

        public void clear() {
            Timer.clear(mRunTimeout);
        }
    }
}
