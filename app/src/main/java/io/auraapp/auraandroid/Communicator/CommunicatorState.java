package io.auraapp.auraandroid.Communicator;

import java.io.Serializable;

public class CommunicatorState implements Serializable {

    public boolean mBtEnabled = false;
    public boolean mBleSupported = false;
    public boolean mAdvertisingSupported = false;

    public boolean mHasPermission = false;

    public boolean mShouldCommunicate = false;

    public boolean mAdvertising = false;
    public boolean mScanning = false;
    public int mRecentBtTurnOnEvents = 0;
    public boolean mBtTurningOn = false;

    @Override
    public String toString() {
        return "CommunicatorState{" +
                "mBtEnabled=" + mBtEnabled +
                ", mBleSupported=" + mBleSupported +
                ", mAdvertisingSupported=" + mAdvertisingSupported +
                ", mHasPermission=" + mHasPermission +
                ", mShouldCommunicate=" + mShouldCommunicate +
                ", mAdvertising=" + mAdvertising +
                ", mScanning=" + mScanning +
                ", mRecentBtTurnOnEvents=" + mRecentBtTurnOnEvents +
                ", mBtTurningOn=" + mBtTurningOn +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommunicatorState that = (CommunicatorState) o;

        if (mBtEnabled != that.mBtEnabled) return false;
        if (mBleSupported != that.mBleSupported) return false;
        if (mAdvertisingSupported != that.mAdvertisingSupported) return false;
        if (mHasPermission != that.mHasPermission) return false;
        if (mShouldCommunicate != that.mShouldCommunicate) return false;
        if (mAdvertising != that.mAdvertising) return false;
        return mScanning == that.mScanning;
    }
}
