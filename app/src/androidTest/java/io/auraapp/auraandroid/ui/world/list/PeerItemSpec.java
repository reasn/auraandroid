package io.auraapp.auraandroid.ui.world.list;

import org.junit.Test;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

import static org.junit.Assert.assertNotEquals;

public class PeerItemSpec {

    private static Peer peerA = new Peer(1234);

    static {
        peerA.mName = "wundula";
        peerA.mSlogans.add(Slogan.create("foo"));
        peerA.mSlogans.add(Slogan.create("fufu"));
    }

    private static Peer peerB = new Peer(1234);

    static {
        peerB.mName = "madeIn";
        peerB.mSlogans.add(Slogan.create("It's summer!"));
        peerB.mSlogans.add(Slogan.create("mobile pay"));
    }

    @Test
    public void should_correctly_determine_equality() {

        PeerItem a = new PeerItem(peerA);
        a.mExpanded = false;
        PeerItem aExpanded = new PeerItem(peerA);
        aExpanded.mExpanded = true;
        assertNotEquals(a, aExpanded);

        assertNotEquals(new PeerItem(peerA), new PeerItem(peerB));
    }
}
