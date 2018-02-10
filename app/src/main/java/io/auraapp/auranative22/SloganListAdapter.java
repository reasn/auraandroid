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

public class SloganListAdapter extends ArrayAdapter<ListItem> {
    private List<ListItem> mItems;

    public SloganListAdapter(@NonNull Context context, List<ListItem> items) {
        super(context, R.layout.list_item, items);
        mItems = items;
    }

    @Override
    public void notifyDataSetChanged() {
        Collections.sort(mItems, (ListItem o1, ListItem o2) -> {
            if (o1.isMine() && !o2.isMine()) {
                return -1;
            }
            if (!o1.isMine() && o2.isMine()) {
                return 1;
            }
            return o1.getSlogan().compareTo(o2.getSlogan());
        });
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
