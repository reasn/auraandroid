package io.auraapp.auraandroid.ui.profile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.DialogManager;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.CommunicatorStateRenderer;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;

import static android.content.Context.MODE_PRIVATE;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class ProfileFragment extends ScreenFragment implements FragmentWithToolbarButtons, FragmentCameIntoView {

    private static final String TAG = "@aura/ui/profile/fragment";
    private RecyclerView mSlogansRecyclerView;
    private ViewGroup mRootView;
    private InfoBox mSlogansInfoBox;
    private final Handler mHandler = new Handler();
    private TextView mNameView;
    private EditText mTextView;
    private LinearLayout mColorWrapper;
    private MySlogansRecycleAdapter mAdapter;
    private CommunicatorState mCommunicatorState;
    private DialogManager mDialogManager;
    private MyProfileManager mMyProfileManager;

    private Runnable hideWelcomeFragments;
    private View.OnClickListener onColorClick;
    private View.OnClickListener onNameClick;
    private View.OnClickListener onTextClick;
    private final MyProfileManager.MyProfileChangedCallback mProfileChangedCallback = event -> {
        switch (event) {
            case MyProfileManager.EVENT_COLOR_CHANGED:
                updateViewsWithColor();
                break;
            case MyProfileManager.EVENT_NAME_CHANGED:
                updateNameAndTextViews();
                break;
            case MyProfileManager.EVENT_TEXT_CHANGED:
                updateNameAndTextViews();
                break;
            case MyProfileManager.EVENT_DROPPED:
            case MyProfileManager.EVENT_ADOPTED:
                // If the numbers of slogans changes, the background has to be adapted
                // to maintain color alternation
                updateViewsWithColor();
                break;
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context $, Intent intent) {
            v(TAG, "onReceive, intent: %s", intent.getAction());
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }
            CommunicatorState state = (CommunicatorState) extras.getSerializable(IntentFactory.INTENT_COMMUNICATOR_EXTRA_STATE);
            if (state != null) {
                v(TAG, "Received new communicator state, state: %s", mCommunicatorState);
                // Intents only have state if it changed
                mCommunicatorState = state;
            }
            reflectCommunicatorState();
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof MainActivity)) {
            throw new RuntimeException("May only attached to " + MainActivity.class.getSimpleName());
        }

        SharedServicesSet servicesSet = ((MainActivity) context).getSharedServicesSet();
        hideWelcomeFragments = () -> servicesSet.mPager.getScreenAdapter().removeWelcomeFragments();

        mMyProfileManager = servicesSet.mMyProfileManager;
        mDialogManager = servicesSet.mDialogManager;

        mMyProfileManager.removeChangedCallback(mProfileChangedCallback);
        mMyProfileManager.addChangedCallback(mProfileChangedCallback);

        onColorClick = $ ->
                mDialogManager.showColorPickerDialog(
                        mMyProfileManager.getProfile().getColor(),
                        mMyProfileManager.getProfile().getColorPickerPointX(),
                        mMyProfileManager.getProfile().getColorPickerPointY(),
                        selected -> {
                            i(TAG, "Changing my color to %s, x: %f, y: %f", selected.getColor(), selected.getPointX(), selected.getPointY());
                            mMyProfileManager.setColor(selected);
                        });

        onNameClick = $ ->
                mDialogManager.showEditMyNameDialog(
                        mMyProfileManager.getProfile().getName(),
                        name -> mMyProfileManager.setName(name)
                );

        onTextClick = $ ->
                mDialogManager.showEditMyTextDialog(
                        mMyProfileManager.getProfile().getText(),
                        text -> mMyProfileManager.setText(text)
                );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v(TAG, "onCreateView");
        mRootView = (ViewGroup) inflater.inflate(R.layout.profile_fragment, container, false);

        mColorWrapper = mRootView.findViewById(R.id.profile_color_button_wrapper);
        mNameView = mRootView.findViewById(R.id.profile_my_name);
        mTextView = mRootView.findViewById(R.id.profile_my_text);

        mSlogansInfoBox = mRootView.findViewById(R.id.profile_my_slogans_info_box);
        mSlogansRecyclerView = mRootView.findViewById(R.id.profile_slogans_recycler);

        mColorWrapper.setOnClickListener(onColorClick);
        mNameView.setOnClickListener(onNameClick);
        mTextView.setOnClickListener(onTextClick);

        mSlogansRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mSlogansRecyclerView.setNestedScrollingEnabled(false);
        mRootView.findViewById(R.id.profile_add_slogan).setOnClickListener($ -> showAddDialog());

        // EditTexts keep their state and might ignore setText without this setting
        mTextView.setSaveEnabled(false);
        updateNameAndTextViews();
        updateViewsWithColor();

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            getContext().registerReceiver(mReceiver, IntentFactory.communicatorIntentFilter());
            v(TAG, "Receiver registered");
            mCommunicatorState = ((MainActivity) getContext()).getSharedState().mCommunicatorState;
            createAdapter();
        }

        mAdapter.notifyMySlogansChanged(mMyProfileManager.getProfile().getSlogans());
        reflectCommunicatorState();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            getContext().unregisterReceiver(mReceiver);
            v(TAG, "Receiver unregistered");
        }
    }

    @Override
    public void cameIntoView() {
        if (hideWelcomeFragments != null) {
            hideWelcomeFragments.run();
        }
        if (getContext() != null) {
            getContext().getSharedPreferences(Config.PREFERENCES_BUCKET, MODE_PRIVATE)
                    .edit()
                    .putBoolean(getString(R.string.prefs_terms_agreed), true)
                    .apply();
        }
    }

    private void createAdapter() {
        if (mAdapter == null) {
            mAdapter = new MySlogansRecycleAdapter(
                    getContext(),
                    mSlogansRecyclerView,
                    (Slogan slogan, int action) -> {
                        if (action == MySlogansRecycleAdapter.OnMySloganActionCallback.ACTION_EDIT) {
                            mDialogManager.showParametrizedSloganEdit(R.string.ui_profile_dialog_edit_slogan_title,
                                    slogan,
                                    // TODO results in a drop/add animation instead of changing one item.
                                    // Reason is that slogans are indexed by name and therefore ListSynchronizer
                                    // cannot detect edits. Identify slogans by int index (would also get rid of sorting)
                                    // and pass index around
                                    sloganText -> mMyProfileManager.replace(slogan, Slogan.create(sloganText)));
                        }
                        mDialogManager.showDrop(slogan, mMyProfileManager::dropSlogan);
                    },
                    mMyProfileManager);
        }
        if (mSlogansRecyclerView != null) {
            mSlogansRecyclerView.setAdapter(mAdapter);
        }
    }

    private void reflectCommunicatorState() {
        // getContext() was observed to be null after long inactivity of the app
        if (mRootView != null && getContext() != null) {
            CommunicatorStateRenderer.populateInfoBoxWithState(mCommunicatorState,
                    mRootView.findViewById(R.id.profile_status_info_box),
                    mRootView.findViewById(R.id.profile_status_summary),
                    getContext());
        }
    }

    private void updateNameAndTextViews() {
        String name = mMyProfileManager.getProfile().getName();
        String text = mMyProfileManager.getProfile().getText();
        i(TAG,
                "Updating name and text views, name: %s, text: %s",
                name.substring(0, Math.min(10, name.length())),
                text.substring(0, Math.min(10, text.length())));
        mNameView.setText(name);
        mTextView.setText(text);
    }

    private void updateViewsWithColor() {
        int color = Color.parseColor(mMyProfileManager.getColor());
        mColorWrapper.getChildAt(0).setBackgroundColor(color);
        // Make sure that there's an alternation i.e. background and color of last item don't match
        mRootView.setBackgroundColor(
                mMyProfileManager.getProfile().getSlogans().size() % 2 == 0
                        ? color
                        : ColorHelper.getAccent(color));
    }

    private void showAddDialog() {
        if (!mMyProfileManager.spaceAvailable()) {
            toast(R.string.ui_world_toast_cannot_add_no_space_available);
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
