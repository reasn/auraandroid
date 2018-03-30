package io.auraapp.auraandroid.ui.world.list;

import io.auraapp.auraandroid.common.Slogan;

@FunctionalInterface
public interface OnAdoptCallback {
    void onAdoptIntended(Slogan peerSlogan);
}
