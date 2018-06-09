package io.auraapp.auraandroid.ui.world.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.auraapp.auraandroid.common.Peer;

class PeerListHelper {

    private static final Comparator<Peer> mComparator = (a, b) -> {
        if (a.mId == b.mId) {
            // Avoids duplicates when the name changes
            return 0;
        }
        if (a.mName != b.mName) {
            // At least one of them is not null
            if (a.mName == null) {
                // a is below b
                return 1;
            }
            if (b.mName == null) {
                return -1;
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

    static List<Peer> sortAndFilterDuplicates(Collection<Peer> peerSet) {
        ArrayList<Peer> result = new ArrayList<>(peerSet);
        Collections.sort(result, mComparator);
        return result;
    }

    static int replace(List<Object> mutablePeers, Peer peer) {
        int position = -1;
        for (int i = 0; i < mutablePeers.size(); i++) {
            if (mutablePeers.get(i) instanceof Peer && ((Peer) mutablePeers.get(i)).mId == peer.mId) {
                position = i;
                break;
            }
        }
        if (position == -1) {
            return -1;
        }

        mutablePeers.remove(position);
        mutablePeers.add(position, peer);
        return position;
    }
}
