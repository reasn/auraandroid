package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.Prefs;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.DialogManager;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;

import static android.content.Context.MODE_PRIVATE;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_ADOPTED;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_COLOR_CHANGED;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_DROPPED;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_NAME_CHANGED;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_REPLACED;
import static io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager.EVENT_TEXT_CHANGED;

public class ProfileFragment extends ScreenFragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/profile/fragment";
    private SharedPreferences mPrefs;
    private MySlogansRecycleAdapter mRecyclerAdapter;
    private RecyclerView mSlogansRecyclerView;
    private Context mContext;
    private ViewGroup mRootView;
    private MyProfileManager mMyProfileManager;
    private TextView mSlogansHeadingTextView;
    private InfoBox mSlogansInfoBox;
    private DialogManager mDialogManager;
    private final Handler mHandler = new Handler();
    private TextView mNameView;
    private TextView mTextView;
    private Button mColorButtonView;
//    private TextView mColorHeading;
//    private TextView mNameHeading;
//    private TextView mTextHeading;

    public static ProfileFragment create(Context context,
                                         MyProfileManager myProfileManager,
                                         DialogManager dialogManager) {
        ProfileFragment fragment = new ProfileFragment();
        fragment.mContext = context;
        fragment.mMyProfileManager = myProfileManager;
        fragment.mDialogManager = dialogManager;
        fragment.mPrefs = context.getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE);
        return fragment;
    }

    @Override
    @ExternalInvocation
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(
                R.layout.profile_fragment, container, false);

        mColorButtonView = mRootView.findViewById(R.id.color_button);
        mNameView = mRootView.findViewById(R.id.my_name);
        mTextView = mRootView.findViewById(R.id.my_text);

        mColorButtonView.setOnClickListener($ ->
                mDialogManager.showColorPickerDialog(color -> {
                    i(TAG, "Changing my color to %s", color);
                    mMyProfileManager.setColor(color);
                })
        );
        mNameView.setOnClickListener($ ->
                mDialogManager.showEditMyNameDialog(
                        mMyProfileManager.getProfile().getName(),
                        name -> mMyProfileManager.setName(name)
                )
        );
        mTextView.setOnClickListener($ ->
                mDialogManager.showEditMyTextDialog(
                        mMyProfileManager.getProfile().getText(),
                        text -> mMyProfileManager.setText(text)
                )
        );

        bindSlogansViews();

        mMyProfileManager.addAndTriggerChangedCallback(
                new int[]{EVENT_COLOR_CHANGED, EVENT_NAME_CHANGED, EVENT_TEXT_CHANGED, EVENT_DROPPED},
                event -> {
                    switch (event) {

                        case EVENT_COLOR_CHANGED:
                            int color = Color.parseColor("#fcfcfc");
                            int textColor = ColorHelper.getTextColor(color);
                            mNameView.setBackgroundColor(color);
                            mColorButtonView.setBackgroundColor(Color.parseColor(mMyProfileManager.getColor()));
                            mTextView.setBackgroundColor(color);
                            mSlogansRecyclerView.setBackgroundColor(color);

                            mRootView.setBackgroundColor(Color.parseColor("#ffffff"));
                            break;

                        case EVENT_NAME_CHANGED:
                            mNameView.setText(mMyProfileManager.getProfile().getName());
                            break;

                        case EVENT_TEXT_CHANGED:
//                            mTextView.setText(EmojiHelper.replaceShortCode("Hallo gafdsafsd sf dsfds" +
//                                    ":thought_balloon::fire::thought_balloon::heart:"));
                            mTextView.setText(mMyProfileManager.getProfile().getText());
                            break;

                        case EVENT_DROPPED:
                        case EVENT_ADOPTED:
                        case EVENT_REPLACED:
                            // Attention: First argument to addAndTriggerChangedCallback relies on joint handling
                            // of aforementioned events.
                            mRecyclerAdapter.notifyMySlogansChanged(mMyProfileManager.getProfile().getSlogans());
                            updateSlogansHeadline();
                    }
                });

        mMyProfileManager.addChangedCallback(event -> {
            switch (event) {
                case MyProfileManager.EVENT_ADOPTED:
                    toast(R.string.ui_profile_toast_slogan_adopted);
                    break;
                case MyProfileManager.EVENT_REPLACED:
                    toast(R.string.ui_profile_toast_slogan_replaced);
                    break;
                case MyProfileManager.EVENT_DROPPED:
                    toast(R.string.ui_profile_toast_slogan_dropped);
                    break;
                case MyProfileManager.EVENT_COLOR_CHANGED:
                    break;
                case MyProfileManager.EVENT_NAME_CHANGED:
                    toast(R.string.ui_profile_toast_name_changed);
                    break;
                case MyProfileManager.EVENT_TEXT_CHANGED:
                    toast(R.string.ui_profile_toast_text_changed);
                    break;
                default:
                    throw new RuntimeException("Unknown slogan event " + event);
            }
        });

        return mRootView;
    }

    private void bindSlogansViews() {

//        mSlogansHeadingTextView = mRootView.findViewById(R.id.my_slogans_heading);
        mSlogansInfoBox = mRootView.findViewById(R.id.my_slogans_info_box);
        mSlogansRecyclerView = mRootView.findViewById(R.id.list_view);

        mSlogansRecyclerView.setNestedScrollingEnabled(false);

        updateSlogansHeadline();

        final FloatingActionButton addSloganButton = mRootView.findViewById(R.id.add_slogan);
        addSloganButton.setOnClickListener($ -> showAddDialog());

        mSlogansRecyclerView.setBackgroundColor(ColorHelper.getAccent(Color.parseColor(mMyProfileManager.getColor())));

        mRecyclerAdapter = new MySlogansRecycleAdapter(mContext,
                mSlogansRecyclerView,
                (Slogan slogan, int action) -> {
                    if (action == MySlogansRecycleAdapter.OnMySloganActionCallback.ACTION_EDIT) {
                        mDialogManager.showParametrizedSloganEdit(R.string.ui_dialog_edit_slogan_title,
                                R.string.ui_dialog_edit_slogan_text,
                                R.string.ui_dialog_edit_slogan_confirm,
                                R.string.ui_dialog_edit_slogan_cancel,
                                slogan,
                                sloganText -> mMyProfileManager.replace(slogan, Slogan.create(sloganText)));
                    }
                    mDialogManager.showDrop(slogan, mMyProfileManager::dropSlogan);
                },
                mMyProfileManager);

        mSlogansRecyclerView.setAdapter(mRecyclerAdapter);
        mSlogansRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mRecyclerAdapter.notifyMySlogansChanged(mMyProfileManager.getProfile().getSlogans());
    }

    private void updateSlogansHeadline() {

        if (2 > 1) {
            return;
        }
        mSlogansHeadingTextView.setText(EmojiHelper.replaceShortCode(mContext.getResources().getQuantityString(
                R.plurals.ui_main_my_slogans_heading,
                mMyProfileManager.getProfile().getSlogans().size(),
                mMyProfileManager.getProfile().getSlogans().size()
        )));
        if (mMyProfileManager.getProfile().getSlogans().size() > 0) {
            mSlogansInfoBox.setVisibility(View.GONE);
            return;
        }

        mSlogansInfoBox.setEmoji(":eyes:");
        mSlogansInfoBox.setHeading(R.string.ui_main_my_slogans_info_no_slogans_heading);
        mSlogansInfoBox.setText(R.string.ui_main_my_slogans_info_no_slogans_text);
        mSlogansInfoBox.setColor(R.color.infoBoxNeutral);
        mSlogansInfoBox.setVisibility(View.VISIBLE);
    }


    private void showAddDialog() {
        if (!mMyProfileManager.spaceAvailable()) {
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
                        mMyProfileManager.adopt(Slogan.create(sloganText));
                    }
                });
    }
}
