package io.auraapp.auranative22;

class Slogan {
    boolean mMine;
    String mText;

    static Slogan create(boolean mine, String text) {
        Slogan s = new Slogan();
        s.mMine = mine;
        s.mText = text;
        return s;

    }
}
