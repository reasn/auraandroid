package io.auraapp.auraandroid.common;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.concurrent.CountDownLatch;

public class TestUtil {
    static void testWithLooper(LooperCallback callback) {
        HandlerThread testThread = new HandlerThread("testThreadedDesign thread");
        testThread.start();

        final CountDownLatch signal = new CountDownLatch(1);
        callback.runWithLooper(new Handler(testThread.getLooper()), signal::countDown);

        try {
            signal.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted" + e.getMessage());
        }
        testThread.getLooper().quit();
    }

    @FunctionalInterface
    interface LooperCallback {
        void runWithLooper(Handler handler, Runnable done);
    }
}
