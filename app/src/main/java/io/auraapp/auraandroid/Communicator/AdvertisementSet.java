package io.auraapp.auraandroid.Communicator;

import java.nio.charset.Charset;
import java.util.UUID;

class AdvertisementSet {

    static final UUID[] ADVERTISED_UUIDS = new UUID[]{
            UuidSet.USER,
            UuidSet.SLOGAN_1,
            UuidSet.SLOGAN_2,
            UuidSet.SLOGAN_3
    };

    class UnknownAdvertisementException extends Exception {
        UnknownAdvertisementException(String message) {
            super(message);
        }
    }

    private byte[][] mSlogans = new byte[3][0];

    void setSlogans(String[] slogans) {

        for (int i = 0; i < 3; i++) {

            mSlogans[i] = slogans.length >= i && slogans[i] != null
                    ? slogans[i].getBytes(Charset.forName("UTF-8"))
                    : new byte[0];
        }
    }

    byte[] getChunkedResponsePayload(UUID uuid) throws UnknownAdvertisementException {
        if (UuidSet.SLOGAN_1.equals(uuid)) {
            return mSlogans[0];

        } else if (UuidSet.SLOGAN_2.equals(uuid)) {
            return mSlogans[1];

        } else if (UuidSet.SLOGAN_3.equals(uuid)) {
            return mSlogans[2];
        }
        throw new UnknownAdvertisementException("Unknown advertisement with uuid " + uuid.toString());
    }
}
