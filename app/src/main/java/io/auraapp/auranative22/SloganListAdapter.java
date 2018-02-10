package io.auraapp.auranative22;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import io.auraapp.auranative22.Communicator.Slogan;

import static io.auraapp.auranative22.FormattedLog.d;

public class SloganListAdapter extends ArrayAdapter<ListItem> {
    private static final String TAG = "@aura/" + SloganListAdapter.class.getSimpleName();
    private final TreeSet<Slogan> mMySlogans;
    private final TreeSet<Slogan> mPeerSlogans;

    private final List<ListItem> mItems;

    static SloganListAdapter create(@NonNull Context context, TreeSet<Slogan> mySlogans, TreeSet<Slogan> peerSlogans) {
        return new SloganListAdapter(context, new ArrayList<>(), mySlogans, peerSlogans);
    }

    private SloganListAdapter(@NonNull Context context, List<ListItem> items, TreeSet<Slogan> mySlogans, TreeSet<Slogan> peerSlogans) {
        super(context, R.layout.list_item, items);
        mItems = items;
        mMySlogans = mySlogans;
        mPeerSlogans = peerSlogans;
    }

    @Override
    public void notifyDataSetChanged() {
        d(TAG, "Updating list, mySlogans: %d, peerSlogans: %d", mMySlogans.size(), mPeerSlogans.size());
        mItems.clear();

        for (Slogan mySlogan : mMySlogans) {
            mItems.add(new ListItem(mySlogan, true));
        }
        for (Slogan peerSlogan : mPeerSlogans) {
            mItems.add(new ListItem(peerSlogan, false));
        }

        super.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        ListItem item = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.slogan_text);
        if (item != null) {
            textView.setText((item.isMine() ? "📝" : "💙") + " " + item.getSlogan().getText());
        } else {
            textView.setText("");
        }
        return convertView;
    }
}
