package io.auraapp.auraandroid.main;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

class PeerMapTransformer {

    static TreeMap<String, PeerSlogan> buildMapFromPeerAndPreviousMap(Peer peer, Map<String, PeerSlogan> immutableMap) {
        TreeMap<String, PeerSlogan> map = new TreeMap<>(immutableMap);

        for (Slogan slogan : peer.mSlogans) {
            String key = slogan.getText();
            PeerSlogan entry = map.get(key);
            if (entry == null) {
                entry = new PeerSlogan(slogan);
                map.put(key, entry);
            } else {
                // We must not mutate objects in immutableMap.
                // As we change entry.mPeers below, we make sure that entry is not in immutableMap
                if (entry == immutableMap.get(key)) {
                    entry = new PeerSlogan(entry.mSlogan);
                    map.put(key, entry);
                }
            }
            if (!entry.mPeers.contains(peer)) {
                entry.mPeers.add(peer);
            }
        }

        Set<Runnable> mutations = new HashSet<>();
        // In case a slogan had just peer as peer and peer doesn't have that slogan anymore, remove it
        for (String sloganText : map.keySet()) {
            boolean hasSlogan = false;
            for (Slogan slogan : peer.mSlogans) {
                if (slogan.getText().equals(sloganText)) {
                    hasSlogan = true;
                    break;
                }
            }
            if (!hasSlogan
                    && map.get(sloganText).mPeers.size() == 1
                    && map.get(sloganText).mPeers.iterator().next().mId.equals(peer.mId)) {
                mutations.add(() -> map.remove(sloganText));
            }
        }
        for (Runnable r : mutations) {
            r.run();
        }

        return map;
    }

    static TreeMap<String, PeerSlogan> buildMapFromPeerList(Iterable<Peer> peers) {
        TreeMap<String, PeerSlogan> map = new TreeMap<>();
        for (Peer peer : peers) {
            map = buildMapFromPeerAndPreviousMap(peer, map);
        }
        return map;
    }

//    private void updateMap(Peer updatedPeer, Set<Peer> previousPeers) {
//
//        // TODO test. now. :)
//        // activity syncs map
//
//        // adapter gets map and syncs view
//
//        // Go through map to map keys to update all items that need updating
//        //      Delete items that have 0 peers
//        //      ani-insert items that are being added
//
//        Map<String, PeerSlogan> map = new HashMap<>();
//
//        for (Peer peer : previousPeers) {
//            if (peer.mAddress.equals(updatedPeer.mAddress)) {
//                if (!peer.mSlogans.equals(updatedPeer.mSlogans)) {
//                    // Slogans changed
//                    for (Slogan oldSlogan : peer.mSlogans) {
//                        map.get(oldSlogan.getText()).mPeers.remove(peer);
//                    }
//                    for (Slogan newSlogan : updatedPeer.mSlogans) {
//                        String key = newSlogan.getText();
//                        if (!map.containsKey(key)) {
//                            map.put(key, new PeerSlogan(newSlogan));
//                        }
//                        map.get(key).mPeers.add(peer);
//                    }
//                }
//                peer.updateWith(updatedPeer);
//            }
//        }
//
//        // Iterate through map to delete removed entries
//
//        Set<String> keysToRemove = new HashSet<>();
//        for (String key : map.keySet()) {
//            boolean foundAtLeastOnePear = false;
//            for (Peer peerInQuestion : map.get(key).mPeers) {
//                // Search mPeers
//                for (Peer peer : previousPeers) {
//                    if (peer.mAddress.equals(peerInQuestion.mAddress)) {
//                        foundAtLeastOnePear = true;
//                        break;
//                    }
//                }
//            }
//            if (!foundAtLeastOnePear) {
//                keysToRemove.add(key);
//            }
//        }
//        for (String key : keysToRemove) {
//            map.remove(key);
//        }
//    }
}
