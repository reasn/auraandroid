package io.auraapp.auraandroid.ui.common;

import android.support.annotation.Nullable;

import java.io.Serializable;

import io.auraapp.auraandroid.Communicator.CommunicatorState;

public class CommunicatorProxyState implements Serializable {
    @Nullable
    public CommunicatorState mCommunicatorState;
    public boolean mEnabled = false;


    CommunicatorProxyState(@Nullable CommunicatorState state) {
        mCommunicatorState = state;
    }

    @Override
    public String toString() {
        return "CommunicatorProxyState{" +
                "mEnabled=" + mEnabled +
                ", mCommunicatorState=" + mCommunicatorState +
                '}';
    }
}
