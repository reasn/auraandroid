package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.DialogManager;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.MySloganManager;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.world.list.SwipeCallback;
import io.auraapp.auraandroid.ui.world.list.item.ListItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;

public class ProfileFragment extends ScreenFragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/profile/fragment";
    private MySlogansRecycleAdapter mRecyclerAdapter;
    private RecyclerView mRecyclerView;
    private Context mContext;
    private ViewGroup mRootView;
    private MySloganManager mMySloganManager;
    private SwipeCallback.OnSwipedCallback mOnSwipedCallback;
    private TextView mHeadingTextView;
    private InfoBox mMySlogansInfoBox;
    private DialogManager mDialogManager;

    public static ProfileFragment create(Context context,
                                         MySloganManager mySloganManager,
                                         DialogManager dialogManager,
                                         SwipeCallback.OnSwipedCallback onSwipedCallback) {
        ProfileFragment fragment = new ProfileFragment();
        fragment.mContext = context;
        fragment.mMySloganManager = mySloganManager;
        fragment.mDialogManager = dialogManager;
        fragment.mOnSwipedCallback = onSwipedCallback;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(
                R.layout.profile_fragment, container, false);

        bindListView();

        mHeadingTextView = mRootView.findViewById(R.id.my_slogans_heading);
        mMySlogansInfoBox = mRootView.findViewById(R.id.my_slogans_info_box);

        bindMySlogansHeadline();

        final FloatingActionButton addSloganButton = mRootView.findViewById(R.id.add_slogan);
        addSloganButton.setOnClickListener($ -> showAddDialog());

        return mRootView;
    }

    private void bindListView() {

        mRecyclerView = mRootView.findViewById(R.id.list_view);

        mRecyclerView.setNestedScrollingEnabled(false);

        List<ListItem> builtinItems = new ArrayList<>();

        mRecyclerAdapter = new MySlogansRecycleAdapter(mContext, builtinItems, mRecyclerView, mOnSwipedCallback);

        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mMySloganManager.addChangedCallback(event -> {
            d(TAG, "My slogans changed");
            mRecyclerAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
            bindMySlogansHeadline();
        });

        mRecyclerAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
    }

    private void bindMySlogansHeadline() {

        mHeadingTextView.setText(EmojiHelper.replaceShortCode(mContext.getResources().getQuantityString(
                R.plurals.ui_main_my_slogans_heading,
                mMySloganManager.getMySlogans().size(),
                mMySloganManager.getMySlogans().size()
        )));
        if (mMySloganManager.getMySlogans().size() > 0) {
            mMySlogansInfoBox.setVisibility(View.GONE);
            return;
        }

        mMySlogansInfoBox.setEmoji(":eyes:");
        mMySlogansInfoBox.setHeading(R.string.ui_main_my_slogans_info_no_slogans_heading);
        mMySlogansInfoBox.setText(R.string.ui_main_my_slogans_info_no_slogans_text);
        mMySlogansInfoBox.setColor(R.color.infoBoxNeutral);
        mMySlogansInfoBox.setVisibility(View.VISIBLE);
    }


    private void showAddDialog() {
        if (!mMySloganManager.spaceAvailable()) {
            toast(R.string.ui_main_toast_cannot_add_no_space_available);
            return;
        }
        mDialogManager.showParametrizedSloganEdit(R.string.ui_dialog_add_slogan_title,
                R.string.ui_dialog_add_slogan_text,
                R.string.ui_dialog_add_slogan_confirm,
                R.string.ui_dialog_add_slogan_cancel,
                null,
                sloganText -> {
                    if (sloganText.length() == 0) {
                        toast(R.string.ui_main_add_slogan_too_short);
                    } else {
                        mMySloganManager.adopt(Slogan.create(sloganText));
                    }
                });
    }
}
