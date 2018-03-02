package io.auraapp.auraandroid.Communicator;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

class AdvertisementSet {

    static final byte FLAG_AURA_IN_FOREGROUND = 1;
//    public static final byte FLAG_B = 2;  // Binary 00010
//    public static final byte FLAG_C = 4;  // Binary 00100
//    public static final byte FLAG_D = 8;  // Binary 01000

    private int mAttachment = 0;

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

    public void setAuraInForeground(boolean auraInForeground) {
        if (auraInForeground) {
            mAttachment = mAttachment | FLAG_AURA_IN_FOREGROUND;
        } else {
            mAttachment &= ~FLAG_AURA_IN_FOREGROUND;
        }
    }

    void setUser(String user) {
        mUser = user;
    }


    // TODO advertise and test if ongoing data transfers are interrupted if advertisement is restarted frequently
    // TODO use one byte as counter for changes to slogans to only fetch if changed
    byte getAttachment() {
        return (byte) mAttachment;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdvertisementSet that = (AdvertisementSet) o;

        if (mAttachment != that.mAttachment) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(mSlogans, that.mSlogans)) return false;
        return mUser != null ? mUser.equals(that.mUser) : that.mUser == null;
    }

    @Override
    public int hashCode() {
        int result = mAttachment;
        result = 31 * result + Arrays.hashCode(mSlogans);
        result = 31 * result + (mUser != null ? mUser.hashCode() : 0);
        return result;
    }
}
