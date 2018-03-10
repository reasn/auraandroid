package io.auraapp.auraandroid.main;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.CuteHasher;
import io.auraapp.auraandroid.common.Peer;

public class DebugPeersListArrayAdapter extends ArrayAdapter<Peer> {
    public DebugPeersListArrayAdapter(@NonNull Context context, int resource, @NonNull Peer[] peers) {
        super(context, resource, peers);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Peer peer = getItem(position);
        if (peer == null) {
            throw new RuntimeException("Peer must not be null");
        }
        long now = System.currentTimeMillis();
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.debug_peers_list_item, parent, false);
        }
        TextView emojiTextView = convertView.findViewById(R.id.emoji);
        TextView detailsTextView = convertView.findViewById(R.id.details);

        emojiTextView.setText(CuteHasher.hash(peer.mId));

        String text = "";

        if (peer.mSynchronizing) {
            text += "syncs";
        } else {
            text += "is fresh";
        }
        text += ", retrievals: " + peer.mSuccessfulRetrievals;
        text += ", seen: " + Math.round((now - peer.mLastSeenTimestamp) / 1000) + "s ago";
        text += ", errors: " + peer.mErrors;

        detailsTextView.setText(text);

        return convertView;
    }

}
