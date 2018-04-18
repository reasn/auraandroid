package io.auraapp.auraandroid.ui.profile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.DialogManager;
import io.auraapp.auraandroid.ui.FragmentWithToolbarButtons;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.common.CommunicatorStateRenderer;
import io.auraapp.auraandroid.ui.common.InfoBox;
import io.auraapp.auraandroid.ui.common.MonoSpaceText;
import io.auraapp.auraandroid.ui.common.ScreenFragment;
import io.auraapp.auraandroid.ui.permissions.FragmentCameIntoView;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;

import static android.content.Context.MODE_PRIVATE;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;

public class ProfileFragment extends ScreenFragment implements FragmentWithToolbarButtons, FragmentCameIntoView {

    private static final String TAG = "@aura/ui/profile/fragment";
    private RecyclerView mSlogansRecyclerView;
    private InfoBox mSlogansInfoBox;
    private TextView mNameView;
    private MonoSpaceText mTextView;
    private LinearLayout mColorWrapper;
    private MySlogansRecycleAdapter mAdapter;
    private CommunicatorProxyState mCommunicatorState;
    private DialogManager mDialogManager;
    private MyProfileManager mMyProfileManager;
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
            if (extras != null) {
                mCommunicatorState = (CommunicatorProxyState) extras.getSerializable(IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE);
                reflectCommunicatorState();
            }
        }
    };

    @Override
    @LayoutRes
    protected int getLayoutResource() {
        return R.layout.profile_fragment;
    }

    @Override
    protected void onResumeWithContext(MainActivity activity, ViewGroup rootView) {

        LocalBroadcastManager.getInstance(activity).registerReceiver(mReceiver, IntentFactory.createFilter(LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION));
        v(TAG, "Receiver registered");

        bindShared(activity);

        mColorWrapper = rootView.findViewById(R.id.profile_color_button_wrapper);
        mNameView = rootView.findViewById(R.id.profile_my_name);
        mTextView = rootView.findViewById(R.id.profile_my_text);

        mSlogansInfoBox = rootView.findViewById(R.id.profile_my_slogans_info_box);
        mSlogansRecyclerView = rootView.findViewById(R.id.profile_slogans_recycler);

        mColorWrapper.setOnClickListener($ -> mDialogManager.showColorPickerDialog(
                mMyProfileManager.getProfile().getColor(),
                mMyProfileManager.getProfile().getColorPickerPointX(),
                mMyProfileManager.getProfile().getColorPickerPointY(),
                selected -> {
                    i(TAG, "Changing my color to %s, x: %f, y: %f", selected.getColor(), selected.getPointX(), selected.getPointY());
                    mMyProfileManager.setColor(selected);
                }));
        mNameView.setOnClickListener($ -> mDialogManager.showEditMyNameDialog(
                mMyProfileManager.getProfile().getName(),
                name -> mMyProfileManager.setName(name)
        ));
        mTextView.setOnClickListener($ -> mDialogManager.showEditMyTextDialog(
                mMyProfileManager.getProfile().getText(),
                text -> mMyProfileManager.setText(text)
        ));

        rootView.findViewById(R.id.profile_add_slogan).setOnClickListener($ -> showAddDialog());

        updateNameAndTextViews();
        updateViewsWithColor();

        bindSlogansRecycler(activity);

        reflectCommunicatorState();
    }

    private void bindShared(MainActivity activity) {
        SharedServicesSet servicesSet = activity.getSharedServicesSet();
        mDialogManager = servicesSet.mDialogManager;
        mCommunicatorState = servicesSet.mCommunicatorProxy.getState();
        mMyProfileManager = servicesSet.mMyProfileManager;
        mMyProfileManager.removeChangedCallback(mProfileChangedCallback);
        mMyProfileManager.addChangedCallback(mProfileChangedCallback);

    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mReceiver);
        v(TAG, "Receiver unregistered");
    }

    @Override
    public void cameIntoView(MainActivity activity) {
        activity.getSharedServicesSet().mPager.getScreenAdapter().removeWelcomeFragments();
        activity.getSharedPreferences(Config.PREFERENCES_BUCKET, MODE_PRIVATE)
                .edit()
                .putBoolean(getString(R.string.prefs_terms_agreed), true)
                .apply();
    }

    private void bindSlogansRecycler(Context context) {
        if (mAdapter == null) {
            mAdapter = new MySlogansRecycleAdapter(
                    context,
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

        mSlogansRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mSlogansRecyclerView.setNestedScrollingEnabled(false);
        mSlogansRecyclerView.setAdapter(mAdapter);
        mAdapter.notifyMySlogansChanged(mMyProfileManager.getProfile().getSlogans());
    }

    private void reflectCommunicatorState() {
        // getContext() was observed to be null after long inactivity of the app
        if (getRootView() != null && getContext() != null) {
            CommunicatorStateRenderer.populateInfoBoxWithState(mCommunicatorState,
                    getRootView().findViewById(R.id.profile_status_info_box),
                    getRootView().findViewById(R.id.profile_status_summary),
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
        getRootView().setBackgroundColor(
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
