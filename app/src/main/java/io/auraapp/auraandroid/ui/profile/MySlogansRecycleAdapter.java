package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.lists.ItemViewHolder;
import io.auraapp.auraandroid.ui.common.lists.ListItem;
import io.auraapp.auraandroid.ui.common.lists.ListSynchronizer;
import io.auraapp.auraandroid.ui.common.lists.RecyclerAdapter;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_COLOR_CHANGED;

public class MySlogansRecycleAdapter extends RecyclerAdapter {

    private final MyProfileManager mMyProfileManager;

    @FunctionalInterface
    public static interface OnMySloganActionCallback {
        int ACTION_EDIT = 2;
        int ACTION_DROP = 3;

        void onActionTaken(Slogan slogan, int action);
    }

    private static final String TAG = "@aura/" + MySlogansRecycleAdapter.class.getSimpleName();

    private final static int TYPE_MY_SLOGAN = 198;
    private final OnMySloganActionCallback mOnMySloganActionCallback;

    public MySlogansRecycleAdapter(@NonNull Context context,
                                   RecyclerView listView,
                                   OnMySloganActionCallback onMySloganActionCallback,
                                   MyProfileManager myProfileManager) {
        super(context, listView);
        mOnMySloganActionCallback = onMySloganActionCallback;
        mMyProfileManager = myProfileManager;
        mMyProfileManager.addChangedCallback(event -> {
            if (event == EVENT_COLOR_CHANGED) {
                notifyDataSetChanged();
            }
        });
    }

    public void notifyMySlogansChanged(TreeSet<Slogan> mySlogans) {
        d(TAG, "Updating list, mySlogans: %d", mySlogans.size());

        final List<ListItem> newItems = new ArrayList<>();
        for (Slogan mySlogan : mySlogans) {
            newItems.add(new MySloganListItem(mySlogan));
        }
        ListSynchronizer.syncLists(
                mItems,
                newItems,
                this,
                (item, newItem) -> item.compareIndex(newItem) > 0
        );
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MySloganHolder(
                mInflater.inflate(R.layout.profile_list_item_slogan, parent, false),
                mContext,
                mOnMySloganActionCallback,
                this.collapseExpandHandler
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        // Alternating colors
        int color = Color.parseColor(mMyProfileManager.getColor());
        ((MySloganHolder) holder).colorize(
                position % 2 == 0 ? color : ColorHelper.getAccent(color),
                ColorHelper.getTextColor(color)
        );

    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_MY_SLOGAN;
    }
}

