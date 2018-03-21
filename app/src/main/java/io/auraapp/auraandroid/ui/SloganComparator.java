package io.auraapp.auraandroid.ui;

import java.io.Serializable;
import java.util.Comparator;

import io.auraapp.auraandroid.common.Slogan;

public class SloganComparator implements Comparator<Slogan>, Serializable {
    @Override
    public int compare(Slogan o1, Slogan o2) {
        return o1.getText().compareTo(o2.getText());
    }
}
