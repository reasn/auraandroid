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
    static final UUID[] ADVERTISED_SLOGAN_UUIDS = new UUID[]{
            UuidSet.SLOGAN_1,
            UuidSet.SLOGAN_2,
            UuidSet.SLOGAN_3
    };

    private String[] mSlogans = new String[3];
    private String mUser;

    void setSlogans(String[] slogans) {

        for (int i = 0; i < 3; i++) {
            mSlogans[i] = slogans.length <= i || slogans[i] == null
                    ? null
                    : slogans[i];
        }
    }

    void setUser(String user) {
        mUser = user;
    }

    byte[] getChunkedResponsePayload(UUID uuid) throws UnknownAdvertisementException {

        String prop;

        if (UuidSet.USER.equals(uuid)) {
            prop = mUser;

        } else if (UuidSet.SLOGAN_1.equals(uuid)) {
            prop = mSlogans[0];

        } else if (UuidSet.SLOGAN_2.equals(uuid)) {
            prop = mSlogans[1];

        } else if (UuidSet.SLOGAN_3.equals(uuid)) {
            prop = mSlogans[2];
        } else {
            throw new UnknownAdvertisementException(uuid);
        }
        return prop == null
                ? new byte[0]
                : prop.getBytes(Charset.forName("UTF-8"));
    }
}
