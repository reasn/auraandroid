package io.auraapp.auranative22;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.List;

import static io.auraapp.auranative22.Communicator.Communicator.INTENT_PEERS_CHANGED_SLOGANS_FOUND;
import static io.auraapp.auranative22.Communicator.Communicator.INTENT_PEERS_CHANGED_SLOGANS_GONE;
import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.v;
import static io.auraapp.auranative22.FormattedLog.w;

public class PeerSloganUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "@aura/peerSloganReceiver";
    private final List<Slogan> mList;
    private final ArrayAdapter<Slogan> mAdapter;

    public PeerSloganUpdateReceiver(List<Slogan> List, ArrayAdapter<Slogan> Adapter) {
        mList = List;
        mAdapter = Adapter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        v(TAG, "onReceive, intent: %s", intent);

        Bundle extras = intent.getExtras();
        if (extras == null) {
            v(TAG, "Intent has no extras, ignoring it, intent: %s", intent);
            return;
        }
        boolean changed = false;

        w(TAG, "%s | %s", extras.get(INTENT_PEERS_CHANGED_SLOGANS_FOUND), extras.get(INTENT_PEERS_CHANGED_SLOGANS_GONE));

        String[] slogansFound = intent.hasExtra(INTENT_PEERS_CHANGED_SLOGANS_FOUND)
                ? extras.getStringArray(INTENT_PEERS_CHANGED_SLOGANS_FOUND)
                : new String[0];
        if (slogansFound != null) {
            d(TAG, "Received %d newly found slogans: %s", slogansFound.length, slogansFound);
            for (String slogan : slogansFound) {
                Slogan found = Slogan.create(false, slogan);
                if (!mList.contains(found)) {
                    mList.add(found);
                    changed = true;

                }
            }
        }
        String[] slogansGone = intent.hasExtra(INTENT_PEERS_CHANGED_SLOGANS_GONE)
                ? extras.getStringArray(INTENT_PEERS_CHANGED_SLOGANS_GONE)
                : new String[0];
        if (slogansGone != null) {
            d(TAG, "Received %d slogans that are gone: %s", slogansGone.length, slogansGone);
            for (String slogan : slogansGone) {
                Slogan gone = Slogan.create(false, slogan);
                if (mList.contains(gone)) {
                    mList.remove(gone);
                    changed = true;
                }
            }
        }
        if (changed) {
            mAdapter.notifyDataSetChanged();
        }
    }
}
