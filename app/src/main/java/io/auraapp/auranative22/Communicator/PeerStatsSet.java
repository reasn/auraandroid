package io.auraapp.auranative22.Communicator;

import java.util.Locale;

import static java.lang.String.format;

class PeerStatsSet {

    int mSuccessfulRetrievals = 0;
    int mErrors = 0;

    String toLogString() {
        return format(Locale.ENGLISH, "mErrors: %d , mSuccessfulRetrievals: %d", mErrors, mSuccessfulRetrievals);
    }
}
