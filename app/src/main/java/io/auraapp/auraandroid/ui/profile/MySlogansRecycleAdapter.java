package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.TreeSet;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.lists.ExpandableRecyclerAdapter;
import io.auraapp.auraandroid.ui.common.lists.ExpandableViewHolder;
import io.auraapp.auraandroid.ui.common.lists.SpacerHolder;
import io.auraapp.auraandroid.ui.common.lists.SpacerItem;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;
import io.auraapp.auraandroid.ui.world.list.PeersDiffCallback;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_ADOPTED;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_COLOR_CHANGED;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_DROPPED;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_REPLACED;

public class MySlogansRecycleAdapter extends ExpandableRecyclerAdapter {


    @FunctionalInterface
    public interface OnMySloganActionCallback {
        int ACTION_EDIT = 2;
        int ACTION_DROP = 3;

        void onActionTaken(Slogan slogan, int action);
    }

    private static final String TAG = "@aura/" + MySlogansRecycleAdapter.class.getSimpleName();

    private final static int TYPE_SPACER = 197;
    private final static int TYPE_MY_SLOGAN = 198;
    private final MyProfileManager mMyProfileManager;
    private final Context mContext;
    private final OnMySloganActionCallback mOnMySloganActionCallback;

    public MySlogansRecycleAdapter(@NonNull Context context,
                                   RecyclerView listView,
                                   OnMySloganActionCallback onMySloganActionCallback,
                                   MyProfileManager myProfileManager) {
        super(context, listView);
        mContext = context;
        mOnMySloganActionCallback = onMySloganActionCallback;
        mMyProfileManager = myProfileManager;
        mItems.add(new SpacerItem());

        mMyProfileManager.addChangedCallback(event -> {

            if (event == EVENT_COLOR_CHANGED) {
                notifyDataSetChanged();

            } else if (event == EVENT_ADOPTED
                    || event == EVENT_DROPPED
                    || event == EVENT_REPLACED) {
                i(TAG, "Slogans changes (%s), synchronizing view", MyProfileManager.nameEvent(event));
                notifyMySlogansListChanged(mMyProfileManager.getProfile().getSlogans());
            }
        });
    }

    public void notifyMySlogansListChanged(TreeSet<Slogan> mySlogans) {
        d(TAG, "Updating list, mySlogans: %d", mySlogans.size());

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new PeersDiffCallback(mItems, new ArrayList<>(mySlogans)));
        mItems.clear();
        mItems.addAll(mySlogans);
        mItems.add(new SpacerItem());
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ExpandableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SPACER) {
            return new SpacerHolder(mInflater.inflate(R.layout.common_list_spacer, parent, false));
        }
        return new MySloganHolder(
                mInflater.inflate(R.layout.profile_list_item_slogan, parent, false),
                mContext,
                mOnMySloganActionCallback
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ExpandableViewHolder holder, int position) {
        if (holder instanceof SpacerHolder) {
            return;
        }

        // Alternating colors
        // The first slogan is colored with accent because because for white background it otherwise
        // would be indistinguishable from my text
        final int color = position % 2 == 0
                ? ColorHelper.getAccent(Color.parseColor(mMyProfileManager.getColor()))
                : Color.parseColor(mMyProfileManager.getColor());

        MySloganHolder castHolder = ((MySloganHolder) holder);
        castHolder.mBackgroundColor = color;
        castHolder.mTextColor = ColorHelper.getTextColor(color);

        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof SpacerItem) {
            return TYPE_SPACER;
        }
        return TYPE_MY_SLOGAN;
    }
}

