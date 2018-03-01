package io.auraapp.auraandroid.common;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;

public class Timer {

    public class Timeout {
        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    private final Handler mHandler;

    public Timer(Handler handler) {
        this.mHandler = handler;
    }

    public Timeout set(Runnable runnable, long millis) {

        Message message = Message.obtain(mHandler, runnable);
        Timeout timeout = new Timeout();
        message.obj = timeout;
        mHandler.sendMessageDelayed(message, millis);

        return timeout;
    }

    public void clear(@Nullable Timeout timeout) {
        if (timeout != null) {
            mHandler.removeCallbacksAndMessages(timeout);
        }
    }
}
