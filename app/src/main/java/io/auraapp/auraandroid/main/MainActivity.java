package io.auraapp.auraandroid.main;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.Communicator.Communicator;
import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.PermissionMissingActivity;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.SloganComparator;
import io.auraapp.auraandroid.main.list.RecycleAdapter;
import io.auraapp.auraandroid.main.list.SwipeCallback;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";

    static final String PREFS_BUCKET = "prefs";
    static final String PREFS_SLOGANS = "slogans";
    private static final String PREFS_ENABLED = "enabled";
    private static final String PREFS_HIDE_BROKEN_BT_STACK_WARNING = "hideBrokenBtStackWarning";
    private static final int BROKEN_BT_STACK_ALERT_DEBOUNCE = 1000 * 60;

    final private TreeSet<Slogan> mPeerSlogans = new TreeSet<>(new SloganComparator());
    private RecycleAdapter mListAdapter;
    private MySloganManager mMySloganManager;
    private CommunicatorProxy mCommunicatorProxy;
    private boolean mAuraEnabled;
    private boolean inForeground = false;
    private SharedPreferences mPrefs;

    private long mBrokenBtStackLastVisibleTimestamp;
    private boolean mBrokenBtStackAlertVisible = false;
    private CommunicatorState mCommunicatorState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        setContentView(R.layout.activity_main);

        // TODO swipe instead of expand
        // TODO refresh, direct Feedback
        // TODO ? debug view shat neatly shows interactions with other devices

        // Create toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_launcher);

        // Load preferences
        mPrefs = getSharedPreferences(MainActivity.PREFS_BUCKET, MODE_PRIVATE);
        mAuraEnabled = mPrefs.getBoolean(MainActivity.PREFS_ENABLED, true);

        final Button addSloganButton = findViewById(R.id.add_slogan);
        addSloganButton.setText(EmojiHelper.replaceShortCode(getString(R.string.ui_main_add_slogan)));
        addSloganButton.setOnClickListener(this::showAddDialog);

        mMySloganManager = new MySloganManager(
                this,
                (int event) -> {
                    d(TAG, "My slogans changed");
                    mListAdapter.notifySlogansChanged();
                    mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
                    switch (event) {
                        case MySloganManager.EVENT_ADOPTED:
                            Toast.makeText(this, R.string.ui_main_toast_adopted, Toast.LENGTH_SHORT).show();
                            break;
                        case MySloganManager.EVENT_REPLACED:
                            Toast.makeText(this, R.string.ui_main_toast_replaced, Toast.LENGTH_SHORT).show();
                            break;
                        case MySloganManager.EVENT_DROPPED:
                            Toast.makeText(this, R.string.ui_main_toast_dropped, Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
        );

        mCommunicatorProxy = new CommunicatorProxy(
                this,
                (Set<Peer> peers) -> {

                    final Set<Slogan> uniqueSlogans = new TreeSet<>();
                    for (Peer peer : peers) {
                        uniqueSlogans.addAll(peer.mSlogans);
                    }
                    v(TAG, "Syncing %d previous slogans to %d slogans from %d peers", mPeerSlogans.size(), uniqueSlogans.size(), peers.size());
                    if (mPeerSlogans.retainAll(uniqueSlogans) || mPeerSlogans.addAll(uniqueSlogans)) {
                        mListAdapter.notifySlogansChanged();
                        reflectCommunicatorState();
                    }
                },
                (CommunicatorState state) -> {
                    mCommunicatorState = state;
                    reflectCommunicatorState();
                });

//        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        createListView();

        mMySloganManager.init();
        mListAdapter.notifySlogansChanged();
        mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
    }

    private void showAddDialog(View $) {
        if (!mMySloganManager.spaceAvailable()) {
            Toast.makeText(this, R.string.ui_main_toast_cannot_add_no_space_available, Toast.LENGTH_LONG).show();
            return;
        }
        showParametrizedSloganEditDialog(R.string.ui_dialog_add_slogan_title,
                R.string.ui_dialog_add_slogan_text,
                R.string.ui_dialog_add_slogan_confirm,
                R.string.ui_dialog_add_slogan_cancel,
                null,
                (String sloganText) -> mMySloganManager.adopt(Slogan.create(sloganText)));
    }

    private void showEditDialog(Slogan slogan) {
        showParametrizedSloganEditDialog(R.string.ui_dialog_edit_slogan_title,
                R.string.ui_dialog_edit_slogan_text,
                R.string.ui_dialog_edit_slogan_confirm,
                R.string.ui_dialog_edit_slogan_cancel,
                slogan,
                (String sloganText) -> mMySloganManager.replace(slogan, Slogan.create(sloganText)));
    }

    interface OnSloganEditConfirm {
        void onConfirm(String text);
    }

    private void showParametrizedSloganEditDialog(@StringRes int title,
                                                  @StringRes int message,
                                                  @StringRes int confirm,
                                                  @StringRes int cancel,
                                                  @Nullable Slogan slogan,
                                                  OnSloganEditConfirm onConfirm) {
        @SuppressLint("InflateParams")
        View dialogView = MainActivity.this.getLayoutInflater().inflate(R.layout.dialog_edit_slogan, null);

        EditText editText = dialogView.findViewById(R.id.dialog_edit_slogan_slogan_text);
        if (slogan != null) {
            editText.setText(slogan.getText());
        }

        AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(message)
                .setView(dialogView)
                .setPositiveButton(confirm, (DialogInterface $$, int $$$) -> onConfirm.onConfirm(editText.getText().toString()))
                .setNegativeButton(cancel, (DialogInterface $$, int $$$) -> {
                })
                .create();
        alert.show();
        if (alert.getWindow() != null) {
            alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        editText.requestFocus();
        editText.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(160),
                (CharSequence source, int start, int end, Spanned dest, int dstart, int dend) -> source.toString().replaceAll("\n", "")
        });
    }

    /**
     * The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
     */
    private void reflectCommunicatorState() {
        String text;

        if (!mCommunicatorState.mHasPermission) {
            throw new RuntimeException("Attempting to render explanation for missing permissions, user should be in MissingPermissionActivity");

        } else if (mCommunicatorState.mBtTurningOn) {
            text = getString(R.string.ui_main_explanation_bt_turning_on);

        } else if (!mCommunicatorState.mBtEnabled) {
            text = getString(R.string.ui_main_explanation_bt_disabled);

        } else if (!mCommunicatorState.mBleSupported) {
            text = getString(R.string.ui_main_explanation_ble_not_supported);

        } else if (!mCommunicatorState.mShouldCommunicate) {
            text = getString(R.string.ui_main_explanation_disabled);

        } else {
            if (!mCommunicatorState.mAdvertisingSupported) {
                text = getString(R.string.ui_main_explanation_advertising_not_supported);
            } else if (!mCommunicatorState.mAdvertising) {
                w(TAG, "Not advertising although it is possible.");
                text = getString(R.string.ui_main_explanation_on_not_active);
            } else if (!mCommunicatorState.mScanning) {
                w(TAG, "Not scanning although it is possible.");
                text = getString(R.string.ui_main_explanation_on_not_active);
            } else if (mPeerSlogans.size() == 0) {
                text = getString(R.string.ui_main_explanation_on_no_peers);
            } else {
                text = getString(R.string.ui_main_explanation_on_peers).replaceAll("##slogans##", Integer.toString(mPeerSlogans.size()));
            }
        }

        TextView view = findViewById(R.id.communicator_state_explanation);

        view.setText(text);
        view.setVisibility(View.VISIBLE);

        if (mCommunicatorState.mRecentBtTurnOnEvents >= Communicator.RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD) {
            showBrokenBtStackAlert();
        }
    }

    private void showBrokenBtStackAlert() {
        if (!inForeground
                || mBrokenBtStackAlertVisible
                || System.currentTimeMillis() - mBrokenBtStackLastVisibleTimestamp > BROKEN_BT_STACK_ALERT_DEBOUNCE
                || mPrefs.getBoolean(MainActivity.PREFS_HIDE_BROKEN_BT_STACK_WARNING, false)) {
            return;
        }
        mBrokenBtStackAlertVisible = true;

        @SuppressLint("InflateParams")
        View dialogView = MainActivity.this.getLayoutInflater().inflate(R.layout.dialog_bt_stack_broken, null);
        CheckBox checkBox = dialogView.findViewById(R.id.dont_show_again);
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.ui_dialog_bt_broken_title)
                .setMessage(R.string.ui_dialog_bt_broken_text)
                .setView(dialogView)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.ui_dialog_bt_broken_confirm, (DialogInterface $$, int $$$) -> {
                    if (checkBox.isChecked()) {
                        mPrefs.edit().putBoolean(MainActivity.PREFS_HIDE_BROKEN_BT_STACK_WARNING, true).apply();
                    }
                    mBrokenBtStackAlertVisible = false;
                    mBrokenBtStackLastVisibleTimestamp = System.currentTimeMillis();
                })
                .setOnDismissListener((DialogInterface $) -> {
                    mBrokenBtStackAlertVisible = false;
                    mBrokenBtStackLastVisibleTimestamp = System.currentTimeMillis();
                })
                .create()
                .show();
    }

    private void createListView() {

        // TODO show stats for slogans
        RecyclerView listView = findViewById(R.id.list_view);

        mListAdapter = RecycleAdapter.create(this, mMySloganManager.getMySlogans(), mPeerSlogans, listView);

        listView.setAdapter(mListAdapter);

        listView.setLayoutManager(new LinearLayoutManager(this));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback(
                this,
                mListAdapter::notifyListItemChanged,
                (Slogan slogan, int action) -> {

                    switch (action) {

                        case SwipeCallback.ACTION_ADOPT:
                            if (mMySloganManager.getMySlogans().contains(slogan)) {
                                Toast.makeText(this, R.string.ui_main_toast_slogan_already_adopted, Toast.LENGTH_LONG).show();
                            } else if (mMySloganManager.spaceAvailable()) {
                                mMySloganManager.adopt(slogan);
                            } else {
                                showReplaceDialog(slogan);
                            }
                            break;

                        case SwipeCallback.ACTION_EDIT:
                            showEditDialog(slogan);
                            break;

                        case SwipeCallback.ACTION_DROP:
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(R.string.ui_drop_dialog_title)
                                    .setIcon(R.mipmap.ic_launcher)
                                    .setMessage(R.string.ui_drop_dialog_message)
                                    .setPositiveButton(R.string.ui_drop_dialog_confirm, (DialogInterface $, int $$) -> {
                                        mMySloganManager.dropSlogan(slogan);
                                    })
                                    .setNegativeButton(R.string.ui_drop_dialog_cancel, (DialogInterface $, int $$) -> {
                                    })
                                    .create()
                                    .show();
                            break;
                    }
                }));
        itemTouchHelper.attachToRecyclerView(listView);
    }

    private void showReplaceDialog(Slogan newSlogan) {
        @SuppressLint("InflateParams")
        View dialogView = MainActivity.this.getLayoutInflater().inflate(R.layout.dialog_replace_slogan, null);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group);

        SparseArray<Slogan> map = new SparseArray<>();

        for (Slogan slogan : mMySloganManager.getMySlogans()) {
            RadioButton button = new RadioButton(this);
            // TODO emoji support
            String text = slogan.getText().length() < 20
                    ? slogan.getText()
                    : slogan.getText().substring(0, 20) + "...";
            button.setText(text);
            int id = View.generateViewId();
            button.setId(id);
            map.put(id, slogan);
            radioGroup.addView(button);
        }

        AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.ui_replace_dialog_title)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(getString(R.string.ui_replace_dialog_message).replaceAll("##maxSlogans##", Integer.toString(MySloganManager.MAX_SLOGANS)))
                .setView(dialogView)
                .setPositiveButton(R.string.ui_replace_dialog_confirm,
                        (DialogInterface $$, int $$$) -> mMySloganManager.replace(map.get(radioGroup.getCheckedRadioButtonId()), newSlogan)
                )
                .setNegativeButton(R.string.ui_replace_dialog_cancel, (DialogInterface $$, int $$$) -> {
                })
                .create();

        alert.show();

        alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        radioGroup.setOnCheckedChangeListener(
                (RadioGroup $, int checkedId) -> alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true)
        );
    }

    /**
     * This is called once, after onCreate
     * https://stackoverflow.com/questions/7705927/android-when-is-oncreateoptionsmenu-called-during-activity-lifecycle
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        MenuItem item = menu.findItem(R.id.enabledSwitch);
        item.setActionView(R.layout.toolbar_switch);

        SwitchCompat enabledSwitch = item.getActionView().findViewById(R.id.switchForActionBar);
        enabledSwitch.setChecked(mAuraEnabled);

        // Managed programmatically because offText XML attribute has no effect for SwitchCompat in menu item
        enabledSwitch.setText(getString(mAuraEnabled
                ? R.string.ui_toolbar_enable_on
                : R.string.ui_toolbar_enable_off));

        enabledSwitch.setOnCheckedChangeListener((CompoundButton $, boolean isChecked) -> {
            mAuraEnabled = isChecked;
            mPrefs.edit().putBoolean(MainActivity.PREFS_ENABLED, isChecked).apply();
            if (isChecked) {
                mCommunicatorProxy.enable();
                enabledSwitch.setText(getString(R.string.ui_toolbar_enable_on));
            } else {
                enabledSwitch.setText(getString(R.string.ui_toolbar_enable_off));
                mCommunicatorProxy.disable();
            }
        });

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!PermissionHelper.granted(this)) {
            Intent intent = new Intent(this, PermissionMissingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            return;
        }

        if (mAuraEnabled) {
            mCommunicatorProxy.enable();
        }
        mCommunicatorProxy.startListening();
        mCommunicatorProxy.askForPeersUpdate();

        inForeground = true;
    }

    @Override
    protected void onPause() {
        mCommunicatorProxy.stopListening();
        inForeground = false;
        super.onPause();
    }
}
