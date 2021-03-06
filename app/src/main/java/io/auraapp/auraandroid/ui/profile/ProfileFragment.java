package io.auraapp.auraandroid.ui.profile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.DialogManager;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.common.ColorHelper;
import io.auraapp.auraandroid.ui.common.fragments.ContextViewFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;

import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_ADOPTED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_DROPPED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_NAME_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_REPLACED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_TEXT_CHANGED_ACTION;

public class ProfileFragment extends ContextViewFragment {

    private static final String TAG = "@aura/ui/profile/fragment";
    private RecyclerView mSlogansRecyclerView;
    private TextView mNameView;
    private TextView mTextView;
    private ColorButton mColorButton;
    private MySlogansRecycleAdapter mAdapter;
    private DialogManager mDialogManager;
    private MyProfileManager mMyProfileManager;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOCAL_MY_PROFILE_COLOR_CHANGED_ACTION.equals(intent.getAction())) {
                updateViewsWithColor();
                mAdapter.notifyDataSetChanged();

            } else if (LOCAL_MY_PROFILE_NAME_CHANGED_ACTION.equals(intent.getAction())
                    || LOCAL_MY_PROFILE_TEXT_CHANGED_ACTION.equals(intent.getAction())) {
                updateNameAndTextViews();

            } else if (LOCAL_MY_PROFILE_ADOPTED_ACTION.equals(intent.getAction())
                    || LOCAL_MY_PROFILE_DROPPED_ACTION.equals(intent.getAction())) {
                // If the numbers of slogans changes, the background has to be adapted
                // to maintain color alternation
                updateViewsWithColor();
                reflectSloganCount();

                i(TAG, "Slogans changes (%s), synchronizing view", intent.getAction());
                mAdapter.notifyMySlogansListChanged(mMyProfileManager.getProfile().getSlogans());
            } else if (LOCAL_MY_PROFILE_REPLACED_ACTION.equals(intent.getAction())) {

                i(TAG, "Slogans changes (%s), synchronizing view", intent.getAction());
                mAdapter.notifyMySlogansListChanged(mMyProfileManager.getProfile().getSlogans());
            }
        }
    };

    @Override
    @LayoutRes
    protected int getLayoutResource() {
        return R.layout.profile_fragment;
    }

    @Override
    protected void onResumeWithContextAndView(MainActivity activity, ViewGroup rootView) {

        bindShared(activity);

        mColorButton = rootView.findViewById(R.id.profile_color_button_wrapper);
        mNameView = rootView.findViewById(R.id.profile_my_name);
        mTextView = rootView.findViewById(R.id.profile_my_text);

        mSlogansRecyclerView = rootView.findViewById(R.id.profile_slogans_recycler);

        // TODO is that the case? untested!
        // Supresses scrolling on dataset changes
        // Thanks https://stackoverflow.com/questions/31860185/nested-recyclerview-scrolls-to-the-first-item-on-notifyitemchanged
        mSlogansRecyclerView.setItemAnimator(null);

        mColorButton.setOnClickListener($ -> mDialogManager.showColorPickerDialog(
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
        reflectSloganCount();

        bindSlogansRecycler(activity, rootView);
    }

    private void bindShared(MainActivity activity) {
        SharedServicesSet servicesSet = activity.getSharedServicesSet();
        mDialogManager = servicesSet.mDialogManager;
        mMyProfileManager = servicesSet.mMyProfileManager;
        LocalBroadcastManager.getInstance(activity).registerReceiver(
                mReceiver,
                IntentFactory.localMyProfileChangedIntentFiler()
        );
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mReceiver);
    }

    private void bindSlogansRecycler(Context context, ViewGroup rootView) {
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
        mAdapter.notifyMySlogansListChanged(mMyProfileManager.getProfile().getSlogans());
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

    private void reflectSloganCount() {
        getRootView().findViewById(R.id.profile_no_slogans).setVisibility(
                mMyProfileManager.getProfile().getSlogans().size() == 0
                        ? View.VISIBLE
                        : View.GONE);
    }

    private void updateViewsWithColor() {
        int color = Color.parseColor(mMyProfileManager.getColor());
        int accent = ColorHelper.getAccent(color);
        mColorButton.setColors(color, accent);
        // Make sure that there's an alternation i.e. background and color of last item don't match.
        // The first slogan is colored with accent because because for white background it otherwise
        // would be indistinguishable from my text
        getRootView().setBackgroundColor(
                mMyProfileManager.getProfile().getSlogans().size() % 2 == 0
                        ? accent
                        : color);
        ((TextView) getRootView().findViewById(R.id.profile_no_slogans)).setTextColor(ColorHelper.getTextColor(color));
    }

    private void showAddDialog() {
        if (!mMyProfileManager.spaceAvailable()) {
            toast(R.string.world_toast_cannot_add_no_space_available);
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
