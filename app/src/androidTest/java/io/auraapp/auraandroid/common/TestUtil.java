package io.auraapp.auraandroid.common;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

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

    /**
     * I want to go back to JS :'(
     * 10min googling didn't help.
     * Supposed to be `assert(actual).to.deep.eq(expected)`
     */
    public static void assertListContents(List<?> actual, List<?> expected) {
        assertEquals(expected.size(), actual.size());
        int i = -1;
        try {
            for (i = 0; i < actual.size(); i++) {
                assertEquals(expected.get(i), actual.get(i));
            }
        } catch (AssertionError error) {
            throw new AssertionError("Item at position " + i + " (of " + expected.size() + ") is not equal expected item:\n" + actual.get(i) + "\n" + expected.get(i) + "");

        }
    }
}
