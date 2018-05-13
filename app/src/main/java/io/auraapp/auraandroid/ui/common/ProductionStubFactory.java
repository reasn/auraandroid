package io.auraapp.auraandroid.ui.common;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;

public class ProductionStubFactory {
    public static Set<Peer> createFakePeers() {
        Set<Peer> peers = new HashSet<>();

        Peer peerA = new Peer(100000123);
        peerA.mColor = "#000000";
        peerA.mName = "Anonymous";

        peerA.mText = "🔥🔥🔥\nDemocracy prevails. Let your kindness be a symbol for humanism and a better future";
        peerA.mSlogans.add(Slogan.create("🕯 Free proxies: 96.12.58.120 and 96.12.82.10."));
        peerA.mSlogans.add(Slogan.create("😷😭 Teargas! Bring water and masks"));
        peerA.mSlogans.add(Slogan.create("Democracy Now!"));
        peers.add(peerA);

        Peer peerB = new Peer(100000124);
        peerB.mColor = "#ffffff";
        peerB.mName = "Jen Benson";

        peerB.mText = "I'm giving a talk on Friday, 2pm:"
                + "\n\"Positive effects of health and happiness\""
                + "\n\nFind me!"
                + "\n@jen.benson"
                + "\nLinkedIn.com/in/jenbenson";
        peerB.mSlogans.add(Slogan.create("#SugarKills"));
        peerB.mSlogans.add(Slogan.create("4pm: Q&A @ speakers corner"));
        peers.add(peerB);

        return peers;
    }
}
