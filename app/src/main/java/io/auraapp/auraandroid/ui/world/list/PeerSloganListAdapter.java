package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;

public class PeerSloganListAdapter extends ArrayAdapter<Slogan> {
    private final PeerItemHolder.ColorSet mColorSet;

    public PeerSloganListAdapter(@NonNull Context context,
                                 int resource,
                                 PeerItemHolder.ColorSet colorSet,
                                 @NonNull List<Slogan> slogans) {
        super(context, resource, slogans.toArray(new Slogan[slogans.size()]));
        mColorSet = colorSet;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Slogan slogan = getItem(position);
        if (slogan == null) {
            throw new RuntimeException("Slogan must not be null");
        }
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.world_peer_item_slogan, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.world_peer_item_slogan_text);
        textView.setText(slogan.getText());

        convertView.setBackgroundColor(position % 2 == 0
                ? mColorSet.mAaccentBackground
                : mColorSet.mBackground);

        textView.setBackgroundColor(position % 2 == 0
                ? mColorSet.mAccentText
                : mColorSet.mText);

        return convertView;
    }

}
