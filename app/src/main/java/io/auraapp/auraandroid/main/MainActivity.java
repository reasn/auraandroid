package io.auraapp.auraandroid.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
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

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.FormattedLog.w;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";

    static final String PREFS_BUCKET = "prefs";
    static final String PREFS_SLOGANS = "slogans";
    static final String PREFS_ENABLED = "enabled";
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
    boolean mBrokenBtStackAlertVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        setContentView(R.layout.activity_main);

        // Create toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_launcher);

        // Load preferences
        mPrefs = getSharedPreferences(MainActivity.PREFS_BUCKET, MODE_PRIVATE);
        mAuraEnabled = mPrefs.getBoolean(MainActivity.PREFS_ENABLED, true);

        mMySloganManager = new MySloganManager(
                this,
                () -> {
                    d(TAG, "My slogans changed");
                    mListAdapter.notifySlogansChanged();
                    // TODO add argument or use added/removed to indicate whether dropped or adopted, update toast
                    Toast.makeText(getApplicationContext(), "Slogan changed ;) ", Toast.LENGTH_LONG).show();
                    mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
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
                    }
                },
                this::updateCommunicatorState);

//        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        Button addSloganButton = findViewById(R.id.add_slogan);
        addSloganButton.setText(EmojiHelper.replaceAppEmoji(getString(R.string.ui_main_add_slogan)));
        addSloganButton.setOnClickListener((View $) -> {

            View dialogView = MainActivity.this.getLayoutInflater().inflate(R.layout.dialog_add_slogan, null);

            EditText editText = dialogView.findViewById(R.id.dialog_add_slogan_slogan_text);

            AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.ui_add_dialog_title)
                    .setIcon(R.mipmap.ic_launcher)
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.ui_add_dialog_confirm), (DialogInterface $$, int $$$) -> {
                        mMySloganManager.adopt(Slogan.create(editText.getText().toString()));
                    })
                    .setNegativeButton(getString(R.string.ui_add_dialog_cancel), (DialogInterface $$, int $$$) -> {
                    })
                    .create();
            alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alert.show();
            editText.requestFocus();
            editText.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(160),
                    (CharSequence source, int start, int end, Spanned dest, int dstart, int dend) -> source.toString().replaceAll("\n", "")
            });
        });
        createListView();

        mMySloganManager.init();
        mListAdapter.notifySlogansChanged();
        mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
    }

    /**
     * The order of conditions should be synchronized with that in Communicator::updateForegroundNotification
     */
    private void updateCommunicatorState(CommunicatorState state) {
        int text;

        if (!state.mHasPermission) {
            throw new RuntimeException("Attempting to render explanation for missing permissions, user should be in MissingPermissionActivity");

        } else if (state.mBtTurningOn) {
            text = R.string.ui_main_explanation_bt_turning_on;

        } else if (!state.mBtEnabled) {
            text = R.string.ui_main_explanation_bt_disabled;

        } else if (!state.mBleSupported) {
            text = R.string.ui_main_explanation_ble_not_supported;

        } else if (!state.mShouldCommunicate) {
            text = R.string.ui_main_explanation_disabled;

        } else {
            if (!state.mAdvertisingSupported) {
                text = R.string.ui_main_explanation_advertising_not_supported;
            } else if (!state.mAdvertising) {
                w(TAG, "Not advertising although it is possible.");
                text = R.string.ui_main_explanation_on_not_active;
            } else if (!state.mScanning) {
                w(TAG, "Not scanning although it is possible.");
                text = R.string.ui_main_explanation_on_not_active;
            } else {
                text = R.string.ui_main_explanation_on;
            }
        }

        TextView view = findViewById(R.id.communicator_state_explanation);

        if (text == -1) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }

        if (state.mRecentBtTurnOnEvents >= Communicator.RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD) {
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

        mListAdapter = RecycleAdapter.create(
                this,
                mMySloganManager.getMySlogans(),
                mPeerSlogans,
                (Slogan slogan) -> {
                    if (mMySloganManager.spaceAvailable()) {
                        mMySloganManager.adopt(slogan);
                    } else {
                        // Replace slogan
                        // TODO implement
                    }
                },
                (Slogan slogan) -> {
                    // TODO edit
                },
                (Slogan slogan) -> {
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
                }
        );

        listView.setAdapter(mListAdapter);

        listView.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * This is called once, after onCreate
     * https://stackoverflow.com/questions/7705927/android-when-is-oncreateoptionsmenu-called-during-activity-lifecycle
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        MenuItem item = menu.findItem(R.id.myswitch);
        item.setActionView(R.layout.appbar_switch);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
