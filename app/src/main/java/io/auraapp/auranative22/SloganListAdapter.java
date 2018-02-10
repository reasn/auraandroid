package io.auraapp.auranative22;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class SloganListAdapter extends ArrayAdapter<Slogan> {
    private List<Slogan> mSlogans;

    public SloganListAdapter(@NonNull Context context, List<Slogan> slogans) {
        super(context, R.layout.list_item, slogans);

        mSlogans = slogans;
    }

    @Override
    public void notifyDataSetChanged() {
        Collections.sort(mSlogans, (Slogan o1, Slogan o2) -> {
            if (o1.mMine && !o2.mMine) {
                return -1;
            }
            if (!o1.mMine && o2.mMine) {
                return 1;
            }
            return o1.mText.compareTo(o2.mText);
        });
        super.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        Slogan slogan = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.slogan_text);
        if (slogan != null) {
            textView.setText((slogan.mMine ? "📝" : "💙") + " " + slogan.mText);
        } else {
            textView.setText("");
        }
        return convertView;
    }
}
