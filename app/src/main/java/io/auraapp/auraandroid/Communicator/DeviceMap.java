package io.auraapp.auraandroid.Communicator;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class DeviceMap {

    /**
     * Id:Device
     */
    private final SparseArray<Device> mDeviceMap = new SparseArray<>();

    /**
     * Address:Id
     */
    private final HashMap<String, Integer> mIdMap = new HashMap<>();

    void setId(String address, int id) {
        mIdMap.put(address, id);
    }

    Collection<Device> values() {
        List<Device> arrayList = new ArrayList<>(mDeviceMap.size());
        for (int i = 0; i < mDeviceMap.size(); i++) {
            arrayList.add(mDeviceMap.valueAt(i));
        }
        return arrayList;
    }

    Collection<Integer> ids() {
        return mIdMap.values();
    }

    Device get(String address) {
        return mDeviceMap.get(mIdMap.get(address));
    }

    Device getById(int id) {
        return mDeviceMap.get(id);
    }

    void put(String address, Device device) {
        mDeviceMap.put(mIdMap.get(address), device);
    }

    void removeById(int id) {
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
        return id != null && mDeviceMap.indexOfKey(id) > -1;
    }
}
