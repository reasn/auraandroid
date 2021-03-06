package io.auraapp.auraandroid.Communicator;

import android.support.annotation.Nullable;

import static io.auraapp.auraandroid.common.FormattedLog.w;

class MetaDataUnpacker {

    static class MetaData {
        final byte mAuraVersion;
        final byte mDataVersion;
        final int mId;

        MetaData(byte auraVersion, byte dataVersion, int id) {
            this.mAuraVersion = auraVersion;
            this.mDataVersion = dataVersion;
            this.mId = id;
        }

        byte getAuraVersion() {
            return mAuraVersion;
        }

        byte getDataVersion() {
            return mDataVersion;
        }

        int getId() {
            return mId;
        }

        @Override
        public String toString() {
            return "MetaData{" +
                    ", mAuraVersion=" + mAuraVersion +
                    ", mDataVersion=" + mDataVersion +
                    ", mId=" + mId +
                    '}';
        }
    }

    private static final String TAG = "aura/communicator/" + MetaDataUnpacker.class.getSimpleName();

    /**
     * Thanks
     * https://stackoverflow.com/questions/5399798/byte-array-and-int-conversion-in-java/11419863
     * https://stackoverflow.com/questions/2817752/java-code-to-convert-byte-to-hexadecimal/21178195
     */
    @Nullable
    static MetaData unpack(byte[] metaData) {

        if (metaData == null) {
            w(TAG, "Additional data missing, null");
            return null;
        } else if (metaData.length < AdvertisementSet.BYTE_COUNT) {
            w(TAG, "Additional data invalid, length: %d", metaData.length);
            return null;
        }

        final byte auraVersion = metaData[0];
        final int id = extractInt(new byte[]{
                metaData[1],
                metaData[2],
                metaData[3],
                metaData[4]
        });
        final byte dataVersion = metaData[5];

        // TODO generate warning/drop/migrate if auraVersion doesn't match

        return new MetaData(auraVersion, dataVersion, id);
    }

    static String byteArrayToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte element : bytes) {
            result.append(" ").append(String.format("%02X", element));
        }
        return result.toString().substring(1);
    }

    private static int extractInt(byte[] array) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (array[i] & 0x000000FF) << shift;
        }
        return value;
    }
}
