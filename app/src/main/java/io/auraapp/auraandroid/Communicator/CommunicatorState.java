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
    public boolean mBluetoothRestartRequired;

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
                ", mBluetoothRestartRequired=" + mBluetoothRestartRequired +
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
        if (mScanning != that.mScanning) return false;
        if (mRecentBtTurnOnEvents != that.mRecentBtTurnOnEvents) return false;
        if (mBtTurningOn != that.mBtTurningOn) return false;
        return mBluetoothRestartRequired == that.mBluetoothRestartRequired;
    }

    @Override
    public int hashCode() {
        int result = (mBtEnabled ? 1 : 0);
        result = 31 * result + (mBleSupported ? 1 : 0);
        result = 31 * result + (mAdvertisingSupported ? 1 : 0);
        result = 31 * result + (mHasPermission ? 1 : 0);
        result = 31 * result + (mShouldCommunicate ? 1 : 0);
        result = 31 * result + (mAdvertising ? 1 : 0);
        result = 31 * result + (mScanning ? 1 : 0);
        result = 31 * result + mRecentBtTurnOnEvents;
        result = 31 * result + (mBtTurningOn ? 1 : 0);
        result = 31 * result + (mBluetoothRestartRequired ? 1 : 0);
        return result;
    }
}
