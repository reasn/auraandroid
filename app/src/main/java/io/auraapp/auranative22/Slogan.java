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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Slogan slogan = (Slogan) o;

        return mMine == slogan.mMine && (
                mText != null
                        ? mText.equals(slogan.mText)
                        : slogan.mText == null
        );
    }
}
