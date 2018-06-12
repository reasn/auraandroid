package io.auraapp.auraandroid.Communicator;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import io.auraapp.auraandroid.common.PeerStatsSet;
import io.auraapp.auraandroid.common.Timer;

class Device {

    final int mId;
    boolean mOutdated = true;
    long lastSeenTimestamp = 0;
    long lastConnectAttempt = 0;

    final PeerStatsSet stats = new PeerStatsSet();

    final PeerBtServiceSet bt = new PeerBtServiceSet();

    boolean isDiscoveringServices = false;

    boolean connected = false;
    boolean shouldDisconnect = false;
    int connectionAttempts = 0;

    boolean mFetchingAProp = false;

    byte mDataVersion = 0;

    private final Map<UUID, Boolean> mFreshMap;
    private final Map<UUID, String> mPropertyMap;
    boolean mSynchronizing = false;
    private Timer.Debouncer debouncer;

    private Device(int id) {
        mFreshMap = new HashMap<>();
        mId = id;

        setAllPropertiesOutdated();

        mPropertyMap = new HashMap<>();

        for (UUID uuid : AdvertisementSet.ADVERTISED_UUIDS) {
            mFreshMap.put(uuid, null);
        }
    }

    static Device create(int id, @NonNull BluetoothDevice btDevice) {
        Device device = new Device(id);
        device.bt.device = btDevice;
        return device;
    }

    Map<UUID, String> props() {
        return mPropertyMap;
    }

    void setAllPropertiesOutdated() {
        for (UUID uuid : AdvertisementSet.ADVERTISED_UUIDS) {
            mFreshMap.put(uuid, false);
        }
    }

    List<String> buildSlogans() {
        List<String> slogans = new ArrayList<>();
        for (UUID uuid : AdvertisementSet.ADVERTISED_SLOGAN_UUIDS) {
            String slogan = mPropertyMap.get(uuid);
            if (slogan != null && !slogan.equals("")) {
                slogans.add(mPropertyMap.get(uuid));
            }
        }
        return slogans;
    }

    @Nullable
    DevicePeerProfile buildProfile() {
        String packed = mPropertyMap.get(UuidSet.PROFILE);
        if (packed == null || packed.equals("")) {
            return null;
        }
        String color = "#" + packed.substring(0, 6);
        String[] parts = packed.substring(6).split(" # ");
        if (parts.length != 2) {
            return null;
            // TODO test with zero-length strings
        }
        String name = parts[0].replaceAll("\\\\#", "#");
        String text = parts[1].replaceAll("\\\\#", "#");

        return new DevicePeerProfile(color, name, text);
    }

    int countSlogans() {
        int count = 0;
        for (UUID uuid : AdvertisementSet.ADVERTISED_SLOGAN_UUIDS) {
            String slogan = mPropertyMap.get(uuid);
            if (slogan != null && !slogan.equals("")) {
                count++;
            }
        }
        return count;
    }

    boolean updateWithReceivedAttribute(UUID uuid, String value) throws UnknownAdvertisementException {

        if (!Arrays.asList(AdvertisementSet.ADVERTISED_UUIDS).contains(uuid)) {
            throw new UnknownAdvertisementException(uuid);
        }

        boolean changed = !Objects.equals(mPropertyMap.get(uuid), value);
        mPropertyMap.put(uuid, value);
        mFreshMap.put(uuid, true);

        return changed;
    }

    UUID getFirstOutdatedPropertyUuid() {

        for (UUID uuid : AdvertisementSet.ADVERTISED_UUIDS) {
            if (!mFreshMap.get(uuid)) {
                return uuid;
            }
        }
        return null;
    }

    Timer.Debouncer getDebouncer(Timer timer, long interval) {
        if (debouncer == null) {
            debouncer = new Timer.Debouncer(timer, interval);
        }
        return debouncer;
    }

    void clearDebouncer() {
        if (debouncer != null) {
            debouncer.clear();
        }
    }

    @Override
    public String toString() {
        return "Device{" +
                "mId='" + mId + '\'' +
                ", mOutdated=" + mOutdated +
                ", lastSeenTimestamp=" + lastSeenTimestamp +
                ", lastConnectAttempt=" + lastConnectAttempt +
                ", stats=" + stats +
                ", bt=" + bt +
                ", isDiscoveringServices=" + isDiscoveringServices +
                ", connected=" + connected +
                ", shouldDisconnect=" + shouldDisconnect +
                ", connectionAttempts=" + connectionAttempts +
                ", mFetchingAProp=" + mFetchingAProp +
                ", mDataVersion=" + mDataVersion +
                ", mFreshMap=" + mFreshMap +
                ", mPropertyMap=" + mPropertyMap +
                ", mSynchronizing=" + mSynchronizing +
                '}';
    }
}
