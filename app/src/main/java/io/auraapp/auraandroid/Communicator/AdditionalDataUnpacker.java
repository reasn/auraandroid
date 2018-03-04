package io.auraapp.auraandroid.Communicator;

import android.bluetooth.le.ScanRecord;

import static io.auraapp.auraandroid.common.FormattedLog.w;

class AdditionalDataUnpacker {

    interface Result {
        boolean isPresent();

        byte getVersion();

        int getId();
    }

    private static final String TAG = "aura/communicator/" + AdditionalDataUnpacker.class.getSimpleName();

    /**
     * Thanks
     * https://stackoverflow.com/questions/5399798/byte-array-and-int-conversion-in-java/11419863
     */
    static Result unpack(ScanRecord scanRecord) {

        Result absent = new Result() {
            @Override
            public boolean isPresent() {
                return false;
            }

            @Override
            public byte getVersion() {
                return 0;
            }

            @Override
            public int getId() {
                return 0;
            }
        };

        if (scanRecord == null) {
            w(TAG, "Scan record is null");
            return absent;
        }
        byte[] additionalData = scanRecord.getServiceData(UuidSet.SERVICE_DATA_PARCEL);
        if (additionalData == null) {
            w(TAG, "Additional data missing, null");
            return absent;
        } else if (additionalData.length < 5) {
            w(TAG, "Additional data invalid, length: %d", additionalData.length);
            return absent;
        }

        final byte version = additionalData[0];

        int value = 0;
        for (int i = 1; i < 5; i++) {
            int shift = (4 - 1 - i + 1) * 8;
            value += (additionalData[i] & 0x000000FF) << shift;
        }
        final int id = value;
        return new Result() {
            @Override
            public boolean isPresent() {
                return true;
            }

            @Override
            public byte getVersion() {
                return version;
            }

            @Override
            public int getId() {
                return id;
            }
        };
    }
}
