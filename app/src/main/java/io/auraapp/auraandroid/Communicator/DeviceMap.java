package io.auraapp.auraandroid.Communicator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

class DeviceMap {

    /**
     * Id:Device
     */
    private final HashMap<Integer, Device> mDeviceMap = new HashMap<>();

    /**
     * Address:Id
     */
    private final HashMap<String, Integer> mIdMap = new HashMap<>();

    void setId(String address, int id) {
        mIdMap.put(address, id);
    }

    Collection<Device> values() {
        return mDeviceMap.values();
    }

    Collection<Integer> ids() {
        return mIdMap.values();
    }

    Device get(String address) {
        return mDeviceMap.get(mIdMap.get(address));
    }

    Device getById(String id) {
        return mDeviceMap.get(id);
    }

    void put(String address, Device device) {
        mDeviceMap.put(mIdMap.get(address), device);
    }

    void clear() {
        mDeviceMap.clear();
        mIdMap.clear();
    }

    void removeById(String id) {
        mDeviceMap.remove(id);
        Set<Runnable> mutations = new HashSet<>();
        for (final String address : mIdMap.keySet()) {
            if (mIdMap.get(address).equals(id)) {
                mutations.add(() -> mIdMap.remove(address));
            }
        }
        for (Runnable r : mutations) {
            r.run();
        }
    }

    boolean has(String address) {
        Integer id = mIdMap.get(address);
        return id != null && mDeviceMap.containsKey(id);
    }
}
