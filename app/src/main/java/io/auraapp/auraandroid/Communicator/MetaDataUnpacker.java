package io.auraapp.auraandroid.Communicator;

import android.support.annotation.Nullable;

import static io.auraapp.auraandroid.common.FormattedLog.w;

class MetaDataUnpacker {

    static class MetaData {
        byte mAuraVersion;
        byte mDataVersion;
        int mId;

        public MetaData(byte auraVersion, byte dataVersion, int id) {
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
        } else if (metaData.length < AdvertisementSet.BYTE_LENGTH) {
            w(TAG, "Additional data invalid, length: %d", metaData.length);
            return null;
        }

        final byte auraVersion = metaData[0];
        final byte dataVersion = metaData[1];
        final int id = extractInt(new byte[]{
                metaData[2],
                metaData[3],
                metaData[4],
                metaData[5]
        });
        // toHexString assumes 4 bytes of output so we chop of the first 2 characters (`substring(2)`)
//        final String color = "#" + Integer.toHexString(extractInt(new byte[]{
//                metaData[1],
//                metaData[2],
//                metaData[3]
//        })).substring(2);

        // TODO generate warning/drop/migrate if auraVersion doesn't match

//        int value = 0;
//        for (int i = 1; i < 5; i++) {
//            int shift = (4 - 1 - i + 1) * 8;
//            value += (metaData[i] & 0x000000FF) << shift;
//        }
//        final int id = value;
        return new MetaData(auraVersion, dataVersion, id);
    }

    public static String byteArrayToString(byte[] bytes) {
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
