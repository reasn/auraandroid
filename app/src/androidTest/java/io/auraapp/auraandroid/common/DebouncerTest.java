package io.auraapp.auraandroid.common;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DebouncerTest {

    @Test
    public void should_run_on_first_invocation() {
        List<String> records = new ArrayList<>();
        TestUtil.testWithLooper((handler, done) -> {
            Timer.Debouncer debouncer = new Timer.Debouncer(new Timer(handler), 10);
            debouncer.debounce(() -> records.add("invocation"));
            handler.postDelayed(done, 20);
        });
        assertThat(records, is(Collections.singletonList("invocation")));
    }

    @Test
    public void should_run_again() {
        List<String> records = new ArrayList<>();
        TestUtil.testWithLooper((handler, done) -> {
            Timer.Debouncer debouncer = new Timer.Debouncer(new Timer(handler), 10);
            Runnable r = () -> records.add("invocation");
            handler.postDelayed(r, 0);
            handler.postDelayed(r, 20);
            handler.postDelayed(r, 30);
            handler.postDelayed(done, 40);
        });
        assertThat(records, is(Arrays.asList("invocation", "invocation", "invocation")));
    }

    @Test
    public void should_filter_in_debounce_interval() {
        List<String> records = new ArrayList<>();
        TestUtil.testWithLooper((handler, done) -> {
            Timer.Debouncer debouncer = new Timer.Debouncer(new Timer(handler), 50);
            for (int delay = 0; delay < 130; delay += 20) {
                final String code = "invocation-" + delay;
                handler.postDelayed(() -> debouncer.debounce(() -> records.add(code)), delay);
            }
            handler.postDelayed(done, 200);
        });
        assertThat(records, is(Arrays.asList("invocation-0", "invocation-60", "invocation-120")));
    }
}
