package io.auraapp.auranative22;

class SimpleSlogan {
    boolean mMine;
    String mText;

    static SimpleSlogan create(boolean mine, String text) {
        SimpleSlogan s = new SimpleSlogan();
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

        SimpleSlogan simpleSlogan = (SimpleSlogan) o;

        return mMine == simpleSlogan.mMine && (
                mText != null
                        ? mText.equals(simpleSlogan.mText)
                        : simpleSlogan.mText == null
        );
    }
}
