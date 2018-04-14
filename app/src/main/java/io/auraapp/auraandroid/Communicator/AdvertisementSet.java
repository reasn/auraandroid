package io.auraapp.auraandroid.Communicator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import io.auraapp.auraandroid.common.Slogan;

import static io.auraapp.auraandroid.common.Config.PROFILE_SLOGANS_MAX_SLOGANS;

class AdvertisementSet {

    static final UUID[] ADVERTISED_UUIDS = new UUID[]{
            UuidSet.PROFILE,
            UuidSet.SLOGAN_1,
            UuidSet.SLOGAN_2,
            UuidSet.SLOGAN_3
    };
    static final UUID[] ADVERTISED_SLOGAN_UUIDS = new UUID[]{
            UuidSet.SLOGAN_1,
            UuidSet.SLOGAN_2,
            UuidSet.SLOGAN_3
    };
    public static final int BYTE_LENGTH = 6;

    int mId = 0;
    byte mVersion = 0;
    String[] mSlogans = new String[PROFILE_SLOGANS_MAX_SLOGANS];
    private String mProfile;
    private String mColor;
    // TODO make number of slogans part of advertisement

    static String[] prepareSlogans(TreeSet<Slogan> slogans) {

        String[] mySloganStrings = new String[slogans.size()];
        int index = 0;
        for (Slogan slogan : slogans) {
            mySloganStrings[index++] = slogan.getText();
        }

        String[] result = new String[PROFILE_SLOGANS_MAX_SLOGANS];
        for (int i = 0; i < PROFILE_SLOGANS_MAX_SLOGANS; i++) {
            result[i] = mySloganStrings.length <= i || mySloganStrings[i] == null
                    ? null
                    : mySloganStrings[i];
        }
        return result;
    }

    public void setColor(String color) {
        this.mColor = color;
    }

    public void setProfile(String profileString) {
        this.mProfile = profileString;
    }

    public static String prepareProfile(String color, String name, String text) {
        if (color.substring(0, 1).equals("#")) {
            color = color.substring(1);
        }
        // Color has a fixed length so we can prepend it without separator
        return color
                + name.replaceAll("#", "\\#")
                + " # " + text.replaceAll("#", "\\#");
    }

    void increaseVersion() {
        mVersion++;
    }

    void shuffleId() {
        while (mId == 0) {
            mId = (int) (Math.round(Math.random() * Integer.MAX_VALUE * 2) - Integer.MAX_VALUE);
        }
    }

    public byte getVersion() {
        return mVersion;
    }

    public String getProfile() {
        return mProfile;
    }

    private static class MetaDataBuilder {
        private final List<Byte> byteList = new ArrayList<>();

        private void add(byte element) {
            byteList.add(element);
        }

        private void add(int integer) {
            for (byte element : ByteBuffer.allocate(4).putInt(integer).array()) {
                byteList.add(element);
            }
        }

        private void add(String string) {

            if (string.substring(0, 1).equals("#")) {
                string = string.substring(1);
            }

            if (string.length() % 2 != 0) {
                throw new Error("Expecting hex string of even length, got " + string.length());
            }
            for (byte element : hexStringToByteArray(string, string.length() / 2)) {
                byteList.add(element);
            }
        }


        private static byte[] hexStringToByteArray(String s, int byteLength) {
            byte[] data = new byte[byteLength];
            for (int byteIndex = 0; byteIndex < byteLength; byteIndex++) {

                int stringIndex = byteIndex * 2;

                char charA = s.length() > stringIndex + 1 ? s.charAt(stringIndex) : "0".charAt(0);
                char charB = s.length() > stringIndex + 1 ? s.charAt(stringIndex + 1) : "0".charAt(0);

                data[byteIndex] = (byte) ((Character.digit(charA, 16) << 4)
                        + Character.digit(charB, 16));
            }
            return data;
        }

        public byte[] build() {
            byte[] result = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                result[i] = byteList.get(i);
            }
            return result;
        }
    }

    byte[] getMetaData() {

        MetaDataBuilder builder = new MetaDataBuilder();

        builder.add(Communicator.PROTOCOL_VERSION);
        builder.add(mId);
        builder.add(mVersion);
//        builder.add(mColor);

        return builder.build();

//        byte[] id = ByteBuffer.allocate(4).putInt(mId).array();
//        byte[] additionalData = Arrays.copyOf(new byte[]{mVersion}, 1 + id.length);
//        System.arraycopy(id, 0, additionalData, 1, id.length);
//        return additionalData;
    }

    // TODO advertise and test if ongoing data transfers are interrupted if advertisement is restarted frequently

    String getProp(UUID uuid)  throws UnknownAdvertisementException {
        if (UuidSet.PROFILE.equals(uuid)) {
            return mProfile;

        } else if (UuidSet.SLOGAN_1.equals(uuid)) {
            return mSlogans[0];

        } else if (UuidSet.SLOGAN_2.equals(uuid)) {
            return mSlogans[1];

        } else if (UuidSet.SLOGAN_3.equals(uuid)) {
            return mSlogans[2];
        } else {
            throw new UnknownAdvertisementException(uuid);
        }
    }

    public String getColor() {
        return mColor;
    }

    @Override
    public String toString() {
        return "AdvertisementSet{" +
                "mId=" + mId +
                ", mVersion=" + mVersion +
                ", mSlogans=" + Arrays.toString(mSlogans) +
                ", mProfile='" + mProfile + '\'' +
                ", mColor='" + mColor + '\'' +
                '}';
    }
}
