package io.auraapp.auraandroid.Communicator;

import android.bluetooth.BluetoothDevice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import io.auraapp.auraandroid.common.PeerStatsSet;

import static java.lang.String.format;

class Device {

    long nextFetch = 0;
    long lastSeenTimestamp = 0;
    long lastConnectAttempt = 0;

    final PeerStatsSet stats = new PeerStatsSet();

    final PeerBtServiceSet bt = new PeerBtServiceSet();

    boolean isDiscoveringServices = false;

    boolean connected = false;
    boolean shouldDisconnect = false;
    int connectionAttempts = 0;

    boolean isFetchingProp = false;

    private final Map<UUID, Boolean> mFreshMap;
    private final Map<UUID, String> mPropertyMap;

    private Device() {
        mFreshMap = new HashMap<>();

        setAllPropertiesOutdated();

        mPropertyMap = new HashMap<>();

        for (UUID uuid : AdvertisementSet.ADVERTISED_UUIDS) {
            mFreshMap.put(uuid, null);
        }
    }

    static Device create(BluetoothDevice btDevice) {
        Device peer = new Device();
        peer.bt.device = btDevice;
        return peer;
    }

    Map<UUID, String> props() {
        return mPropertyMap;
    }

    void setAllPropertiesOutdated() {
        for (UUID uuid : AdvertisementSet.ADVERTISED_UUIDS) {
            mFreshMap.put(uuid, false);
        }
    }

    Set<String> getSlogans() {
        Set<String> slogans = new HashSet<>();
        for (UUID uuid : AdvertisementSet.ADVERTISED_SLOGAN_UUIDS) {
            String slogan = mPropertyMap.get(uuid);
            if (slogan != null && !slogan.equals("")) {
                slogans.add(mPropertyMap.get(uuid));
            }
        }
        return slogans;
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

    String toLogString() {
        return format(
                Locale.ENGLISH,
                "nextFetch: %d"
                        + ", lastSeenTimestamp: %d"
                        + ", lastConnectAttempt: %d"
                        + ", isDiscoveringServices: %s"
                        + ", bt: (%s)"
                        + ", connected: %s"
                        + ", shouldDisconnect: %s"
                        + ", connectionAttempts: %d"
                        + ", stats: (%s)"
                        + ", isFetchingProp: %s"
                        + ", mPropertyMap: %s",
                nextFetch,
                lastSeenTimestamp,
                lastConnectAttempt,
                isDiscoveringServices ? "yes" : "no",
                bt.toLogString(),
                connected ? "yes" : "no",
                shouldDisconnect ? "yes" : "no",
                connectionAttempts,
                stats.toLogString(),
                isFetchingProp ? "yes" : "no",
                mPropertyMap.values().toString()
        );
    }
}
