package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.lists.ExpandableRecyclerAdapter;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;

public class PeerSloganAdapter extends ExpandableRecyclerAdapter {
    ColorSet mColorSet;
    private final OnAdoptCallback mOnAdoptCallback;
    private final PeerSloganHolder.WhatsMyColorCallback mWhatsMyColorCallback;
    private Context mContext;

    public PeerSloganAdapter(@NonNull Context context,
                             RecyclerView listView,
                             ColorSet colorSet,
                             OnAdoptCallback onAdoptCallback,
                             ArrayList<Slogan> slogans,
                             PeerSloganHolder.WhatsMyColorCallback whatsMyColorCallback) {

        super(context, listView);
        mItems.addAll(slogans);
        mColorSet = colorSet;
        mContext = context;
        mOnAdoptCallback = onAdoptCallback;
        mWhatsMyColorCallback = whatsMyColorCallback;
    }


    @Override
    public void onBindViewHolder(@NonNull ExpandableViewHolder holder, int position) {

        PeerSloganHolder castHolder = ((PeerSloganHolder) holder);
        castHolder.mTextColor = mColorSet.mText;
        castHolder.mBackgroundColor = position % 2 == 0
                ? mColorSet.mAccentBackground
                : mColorSet.mBackground;

        super.onBindViewHolder(holder, position);
    }

    @NonNull
    @Override
    public ExpandableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PeerSloganHolder(
                mInflater.inflate(R.layout.world_peer_slogan, parent, false),
                mContext,
                mOnAdoptCallback,
                mWhatsMyColorCallback);
    }

    public List<Object> getItems() {
        return mItems;
    }
}
