package io.auraapp.auraandroid.ui.world.list;

import org.junit.Test;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PeerSpec {

    private static Peer peerA = new Peer(1234);
    private static Peer peerA2 = new Peer(1234);
    private static Peer peerB = new Peer(1234);

    static {
        peerA.mName = "wundula";
        peerA.mSlogans.add(Slogan.create("foo"));
        peerA.mSlogans.add(Slogan.create("fufu"));
    }

    static {
        peerA2.mName = "wundula";
        peerA2.mSlogans.add(Slogan.create("foo"));
        peerA2.mSlogans.add(Slogan.create("fufu"));
    }

    static {
        peerB.mName = "madeIn";
        peerB.mSlogans.add(Slogan.create("It's summer!"));
        peerB.mSlogans.add(Slogan.create("mobile pay"));
    }

    @Test
    public void should_correctly_determine_equality() {

        assertEquals(peerA, peerA2);
        assertNotEquals(peerA, peerB);
    }
}
