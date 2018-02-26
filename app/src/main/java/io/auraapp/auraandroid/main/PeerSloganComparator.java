package io.auraapp.auraandroid.main;

import java.io.Serializable;
import java.util.Comparator;

public class PeerSloganComparator implements Comparator<PeerSlogan>, Serializable {
    @Override
    public int compare(PeerSlogan o1, PeerSlogan o2) {
        return o1.mSlogan.getText().compareTo(o2.mSlogan.getText());
    }
}
