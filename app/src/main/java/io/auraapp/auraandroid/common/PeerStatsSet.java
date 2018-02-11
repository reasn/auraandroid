package io.auraapp.auraandroid.common;

import java.util.Locale;

import static java.lang.String.format;

public class PeerStatsSet {

    public int mSuccessfulRetrievals = 0;
    public int mErrors = 0;

    public String toLogString() {
        return format(Locale.ENGLISH, "mErrors: %d , mSuccessfulRetrievals: %d", mErrors, mSuccessfulRetrievals);
    }
}
