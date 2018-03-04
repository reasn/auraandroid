package io.auraapp.auraandroid.Communicator;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

class AdvertisementSet {

    static final UUID[] ADVERTISED_UUIDS = new UUID[]{
            // User is currently not advertised, see Advertiser#createSloganService()
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

    int mId = 0;
    byte mVersion = 0;
    private String[] mSlogans = new String[3];
    boolean mSlogansSet = false;
    private String mUser;

    void setSlogans(String[] slogans) {
        mSlogansSet = true;
        for (int i = 0; i < 3; i++) {
            mSlogans[i] = slogans.length <= i || slogans[i] == null
                    ? null
                    : slogans[i];
        }
    }

    void setUser(String user) {
        mUser = user;
    }

    void increaseVersion() {
        mVersion++;
    }

    void shuffleId() {
        while (mId == 0) {
            mId = (int) (Math.round(Math.random() * Integer.MAX_VALUE * 2) - Integer.MAX_VALUE);
        }
    }

    byte[] getAdditionalData() {

        byte[] id = ByteBuffer.allocate(4).putInt(mId).array();
        byte[] additionalData = Arrays.copyOf(new byte[]{mVersion}, 1 + id.length);
        System.arraycopy(id, 0, additionalData, 1, id.length);
        return additionalData;
    }

    // TODO advertise and test if ongoing data transfers are interrupted if advertisement is restarted frequently

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
