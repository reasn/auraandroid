package io.auraapp.auraandroid.Communicator;

/**
 * Created by alexander on 14.04.18.
 */
class DevicePeerProfile {
    final String mColor;
    final String mName;
    final String mText;

    public DevicePeerProfile(String mColor, String mName, String mText) {
        this.mColor = mColor;
        this.mName = mName;
        this.mText = mText;
    }

    String getColor() {
        return mColor;
    }

    String getName() {
        return mName;
    }

    String getText() {
        return mText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DevicePeerProfile profile = (DevicePeerProfile) o;

        if (!mColor.equals(profile.mColor)) return false;
        if (!mName.equals(profile.mName)) return false;
        return mText.equals(profile.mText);
    }

    @Override
    public int hashCode() {
        int result = mColor.hashCode();
        result = 31 * result + mName.hashCode();
        result = 31 * result + mText.hashCode();
        return result;
    }
}
