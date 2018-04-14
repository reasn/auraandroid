package io.auraapp.auraandroid.Communicator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class MetaDataBuilder {
    private final List<Byte> byteList = new ArrayList<>();

    void add(byte element) {
        byteList.add(element);
    }

    void add(int integer) {
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
