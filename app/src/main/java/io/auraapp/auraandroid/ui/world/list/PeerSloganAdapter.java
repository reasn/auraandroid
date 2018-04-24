package io.auraapp.auraandroid.ui.world.list;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.ui.common.lists.LegacyItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.LegacyRecyclerAdapter;
import io.auraapp.auraandroid.ui.profile.LegacySpacerHolder;

public class PeerSloganAdapter extends LegacyRecyclerAdapter {
    private final ColorSet mColorSet;
    private final OnAdoptCallback mOnAdoptCallback;

    public PeerSloganAdapter(@NonNull Context context,
                             RecyclerView listView,
                             ColorSet colorSet,
                             OnAdoptCallback onAdoptCallback,
                             ArrayList<PeerSloganItem> sloganItems) {

        super(context, listView);
        mItems.addAll(sloganItems);
        mColorSet = colorSet;
        mOnAdoptCallback = onAdoptCallback;
    }

    @Override
    public void onBindViewHolder(@NonNull LegacyItemViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        if (holder instanceof LegacySpacerHolder) {
            return;
        }
        if (position % 2 == 0) {
            holder.colorize(mColorSet.mAccentBackground, mColorSet.mAccentText);
        } else {
            holder.colorize(mColorSet.mBackground, mColorSet.mText);
        }
    }


    @NonNull
    @Override
    public LegacyItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PeerSloganHolder(
                mInflater.inflate(R.layout.world_peer_slogan, parent, false),
                mContext,
                mCollapseExpandHandler,
                mOnAdoptCallback);
    }
}
