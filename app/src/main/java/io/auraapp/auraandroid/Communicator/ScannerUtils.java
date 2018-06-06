package io.auraapp.auraandroid.Communicator;


import java.util.HashSet;
import java.util.Set;

import static io.auraapp.auraandroid.common.FormattedLog.i;

class ScannerUtils {

    private final static String TAG = "@aura/ble/scanner/utils";

    /**
     * @return true if mMutableDeviceMap has been mutated
     */
    static boolean removeOldDevicesWithSameContent(DeviceMap mMutableDeviceMap) {

        Set<Integer> idsToForget = new HashSet<>();

        for (int id : mMutableDeviceMap.ids()) {
            Device device = mMutableDeviceMap.getById(id);
            boolean moreRecentlySeenDeviceWithSameContentExists = false;

            for (int candidateId : mMutableDeviceMap.ids()) {
                Device candidate = mMutableDeviceMap.getById(candidateId);
                if (device != candidate
                        && !device.connected
                        && candidate.lastSeenTimestamp > device.lastSeenTimestamp
                        && areContentsTheSame(device, candidate)) {
                    moreRecentlySeenDeviceWithSameContentExists = true;
                    break;
                }
            }

            if (moreRecentlySeenDeviceWithSameContentExists) {
                idsToForget.add(id);
            }
        }
        if (idsToForget.size() == 0) {
            return false;
        }
        for (int id : idsToForget) {
            i(TAG, "Forgetting disconnected peer because more recent peer with same content exists, deivce: %d", id);
            mMutableDeviceMap.removeById(id);
        }
        return true;
    }

    /**
     * Ignores mId.
     * Attention: Needs to be maintained when props are changed
     */
    private static boolean areContentsTheSame(Device a, Device b) {
        DevicePeerProfile profileA = a.buildProfile();
        DevicePeerProfile profileB = b.buildProfile();

        if (profileA != null && profileB != null) {

            if (profileA.mColor != null ? !profileA.mColor.equals(profileB.mColor) : profileB.mColor != null)
                return false;
            if (profileA.mText != null ? !profileA.mText.equals(profileB.mText) : profileB.mText != null)
                return false;
            if (profileA.mName != null ? !profileA.mName.equals(profileB.mName) : profileB.mName != null)
                return false;

        } else if (profileA != profileB) {
            return false;
        }

        return a.buildSlogans().equals(b.buildSlogans());
    }
}
