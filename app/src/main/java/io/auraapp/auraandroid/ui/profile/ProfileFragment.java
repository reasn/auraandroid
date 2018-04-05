package io.auraapp.auraandroid.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
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
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class ProfileFragment extends ScreenFragment implements FragmentWithToolbarButtons {

    private static final String TAG = "@aura/ui/profile/fragment";
    private SharedPreferences mPrefs;
    private MySlogansRecycleAdapter mRecyclerAdapter;
    private RecyclerView mSlogansRecyclerView;
    private ViewGroup mRootView;
    private MyProfileManager mMyProfileManager;
    private InfoBox mSlogansInfoBox;
    private DialogManager mDialogManager;
    private final Handler mHandler = new Handler();
    private TextView mNameView;
    private EditText mTextView;
    private LinearLayout mColorWrapper;

    public static ProfileFragment create(Context context,
                                         MyProfileManager myProfileManager,
                                         DialogManager dialogManager) {
        ProfileFragment fragment = new ProfileFragment();
        fragment.setContext(context);
        fragment.mMyProfileManager = myProfileManager;
        fragment.mDialogManager = dialogManager;
        fragment.mPrefs = context.getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE);
        return fragment;
    }

    @Override
    @ExternalInvocation
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");
        mRootView = (ViewGroup) inflater.inflate(R.layout.profile_fragment, container, false);

        mColorWrapper = mRootView.findViewById(R.id.color_button_wrapper);
        mNameView = mRootView.findViewById(R.id.my_name);
        mTextView = mRootView.findViewById(R.id.my_text);

        mSlogansInfoBox = mRootView.findViewById(R.id.my_slogans_info_box);
        mSlogansRecyclerView = mRootView.findViewById(R.id.list_view);

        mTextView.invalidate();

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mColorWrapper.getChildAt(0).setBackgroundColor(Color.parseColor(mMyProfileManager.getColor()));
        mColorWrapper.setOnClickListener($ ->
                mDialogManager.showColorPickerDialog(
                        mMyProfileManager.getProfile().getColor(),
                        mMyProfileManager.getProfile().getColorPickerPointX(),
                        mMyProfileManager.getProfile().getColorPickerPointY(),
                        selected -> {
                            i(TAG, "Changing my color to %s, x: %f, y: %f", selected.getColor(), selected.getPointX(), selected.getPointY());
                            mMyProfileManager.setColor(selected);
                        })
        );

        mNameView.setText(mMyProfileManager.getProfile().getName());
        mNameView.setOnClickListener($ ->
                mDialogManager.showEditMyNameDialog(
                        mMyProfileManager.getProfile().getName(),
                        name -> mMyProfileManager.setName(name)
                )
        );

        mTextView.setText(mMyProfileManager.getProfile().getText());
        mTextView.setOnClickListener($ ->
                mDialogManager.showEditMyTextDialog(
                        mMyProfileManager.getProfile().getText(),
                        text -> mMyProfileManager.setText(text)
                )
        );

        bindSlogansViews();
    }

    private void bindSlogansViews() {

        // TODO add spacer at end to keep recycler from overlapping last item during collapse animation of arbitrary item

        mSlogansRecyclerView.setNestedScrollingEnabled(false);

        final FloatingActionButton addSloganButton = mRootView.findViewById(R.id.add_slogan);
        addSloganButton.setOnClickListener($ -> showAddDialog());

        mSlogansRecyclerView.setBackgroundColor(ColorHelper.getAccent(Color.parseColor(mMyProfileManager.getColor())));

        mRecyclerAdapter = new MySlogansRecycleAdapter(
                getContext(),
                mSlogansRecyclerView,
                (Slogan slogan, int action) -> {
                    if (action == MySlogansRecycleAdapter.OnMySloganActionCallback.ACTION_EDIT) {
                        mDialogManager.showParametrizedSloganEdit(R.string.ui_profile_dialog_edit_slogan_title,
                                slogan,
                                sloganText -> mMyProfileManager.replace(slogan, Slogan.create(sloganText)));
                    }
                    mDialogManager.showDrop(slogan, mMyProfileManager::dropSlogan);
                },
                mMyProfileManager);

        mSlogansRecyclerView.setAdapter(mRecyclerAdapter);
        mSlogansRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mRecyclerAdapter.notifyMySlogansChanged(mMyProfileManager.getProfile().getSlogans());
    }

    private void showAddDialog() {
        if (!mMyProfileManager.spaceAvailable()) {
            toast(R.string.ui_main_toast_cannot_add_no_space_available);
            return;
        }
        mDialogManager.showParametrizedSloganEdit(R.string.ui_profile_dialog_add_slogan_title,
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
