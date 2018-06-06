package io.auraapp.auraandroid.ui.world.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.auraapp.auraandroid.common.Peer;

class PeerDuplicateFilter {

    private static final Comparator<Peer> mComparator = (a, b) -> {
        if (a.mId == b.mId) {
            // Avoids duplicates when the name changes
            return 0;
        }
        if (a.mName != b.mName) {
            // They're not both null
            if (a.mName == null) {
                // a is less than
                return -1;
            }
            if (b.mName == null) {
                return 1;
            }
        }
        if (Math.abs(a.mLastSeenTimestamp - b.mLastSeenTimestamp) < 60000) {
            if (a.mName == null || b.mName == null) {
                return a.mId - b.mId;
            }
            return a.mName.compareTo(b.mName);
        }
        return (int) (a.mLastSeenTimestamp - b.mLastSeenTimestamp);
    };

    static List<Peer> filterDuplicates(Collection<Peer> peerSet) {
        ArrayList<Peer> result = new ArrayList<>(peerSet);
        Collections.sort(result, mComparator);
        return result;
    }
}
