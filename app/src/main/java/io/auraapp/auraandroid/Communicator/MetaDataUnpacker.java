package io.auraapp.auraandroid.Communicator;

import android.bluetooth.le.ScanRecord;

import static io.auraapp.auraandroid.common.FormattedLog.w;

class MetaDataUnpacker {

    static class MetaData {
        boolean mPresent;
        byte mAuraVersion;
        byte mDataVersion;
        int mId;

        public MetaData(boolean present, byte auraVersion, byte dataVersion, int id) {
            this.mPresent = present;
            this.mAuraVersion = auraVersion;
            this.mDataVersion = dataVersion;
            this.mId = id;
        }

        boolean isPresent() {
            return mPresent;
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
                    "mPresent=" + mPresent +
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
    static MetaData unpack(ScanRecord scanRecord) {

        MetaData absent = new MetaData(false, (byte) 0, (byte) 0, 0);

        if (scanRecord == null) {
            w(TAG, "Scan record is null");
            return absent;
        }
        byte[] metaData = scanRecord.getServiceData(UuidSet.SERVICE_DATA_PARCEL);
        if (metaData == null) {
            w(TAG, "Additional data missing, null");
            return absent;
        } else if (metaData.length < AdvertisementSet.BYTE_LENGTH) {
            w(TAG, "Additional data invalid, length: %d", metaData.length);
            return absent;
        }

        final byte auraVersion = metaData[0];
        final byte dataVersion = metaData[0];
        final int id = extractInt(new byte[]{
                metaData[1],
                metaData[2],
                metaData[3],
                metaData[4]
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
        return new MetaData(true, auraVersion, dataVersion, id);
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
