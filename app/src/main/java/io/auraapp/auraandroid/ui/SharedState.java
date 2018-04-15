package io.auraapp.auraandroid.ui;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.common.Peer;

public class SharedState {
    public Set<Peer> mPeers = new HashSet<>();
    public CommunicatorState mCommunicatorState;
}
