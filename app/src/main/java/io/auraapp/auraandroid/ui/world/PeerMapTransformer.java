package io.auraapp.auraandroid.ui.world;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

public class PeerMapTransformer {

    public static TreeMap<String, PeerSlogan> buildMapFromPeerAndPreviousMap(Peer peer, Map<String, PeerSlogan> immutableMap) {
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
                    && map.get(sloganText).mPeers.iterator().next().mId == peer.mId) {
                mutations.add(() -> map.remove(sloganText));
            }
        }
        for (Runnable r : mutations) {
            r.run();
        }

        return map;
    }

    public static TreeMap<String, PeerSlogan> buildMapFromPeerList(Iterable<Peer> peers) {
        TreeMap<String, PeerSlogan> map = new TreeMap<>();
        for (Peer peer : peers) {
            map = buildMapFromPeerAndPreviousMap(peer, map);
        }
        return map;
    }
}
