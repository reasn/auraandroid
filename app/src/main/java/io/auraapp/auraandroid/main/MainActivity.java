package io.auraapp.auraandroid.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.Communicator;
import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.PermissionMissingActivity;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.main.list.RecycleAdapter;
import io.auraapp.auraandroid.main.list.SwipeCallback;
import io.auraapp.auraandroid.main.list.item.ListItem;
import io.auraapp.auraandroid.main.list.item.MySlogansHeadingItem;
import io.auraapp.auraandroid.main.list.item.PeersHeadingItem;
import io.auraapp.auraandroid.main.list.item.StatusItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";

    static final String PREFS_BUCKET = "prefs";
    static final String PREFS_SLOGANS = "slogans";
    private static final String PREFS_ENABLED = "enabled";
    private static final String PREFS_HIDE_BROKEN_BT_STACK_WARNING = "hideBrokenBtStackWarning";
    private static final int BROKEN_BT_STACK_ALERT_DEBOUNCE = 1000 * 60;
    private static final long SWIPE_TO_REFRESH_DURATION = 1000 * 2;

    private RecycleAdapter mListAdapter;
    private StatusItem mStatusItem;
    private PeersHeadingItem mPeersHeadingItem;

    private MySloganManager mMySloganManager;
    private CommunicatorProxy mCommunicatorProxy;
    private boolean mAuraEnabled;
    private boolean inForeground = false;
    private SharedPreferences mPrefs;

    private long mBrokenBtStackLastVisibleTimestamp;
    private CommunicatorState mCommunicatorState;
    private Set<Peer> mPeers = new HashSet<>();
    private final Handler mHandler = new Handler();
    /**
     * slogan:PeerSlogan
     */
    TreeMap<String, PeerSlogan> mPeerSloganMap = new TreeMap<>();
    private MySlogansHeadingItem mMySlogansHeadingItem;
    private DialogManager mDialogManager;

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
        mDialogManager = new DialogManager(this);

        final FloatingActionButton addSloganButton = findViewById(R.id.add_slogan);
        addSloganButton.setOnClickListener(this::showAddDialog);

        mMySloganManager = new MySloganManager(
                this,
                (int event) -> {
                    d(TAG, "My slogans changed");
                    mListAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
                    mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
                    int text;
                    switch (event) {
                        case MySloganManager.EVENT_ADOPTED:
                            text = R.string.ui_main_toast_adopted;
                            break;
                        case MySloganManager.EVENT_REPLACED:
                            text = R.string.ui_main_toast_replaced;
                            break;
                        case MySloganManager.EVENT_DROPPED:
                            text = R.string.ui_main_toast_dropped;
                            break;
                        default:
                            throw new RuntimeException("Unknown slogan event " + event);
                    }
                    Toast.makeText(this, EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
                }
        );
        mCommunicatorProxy = new CommunicatorProxy(
                this,
                (Set<Peer> peers) -> {
                    mPeers = peers;
                    mPeerSloganMap = PeerMapTransformer.buildMapFromPeerList(peers);
                    mListAdapter.notifyPeerSloganListChanged(mPeerSloganMap);
                    reflectStatus();
                },
                (Peer peer) -> {
                    for (Peer candidate : mPeers) {
                        if (candidate.mId.equals(peer.mId)) {
                            mPeers.remove(candidate);
                            break;
                        }
                    }
                    mPeers.add(peer);
                    mPeerSloganMap = PeerMapTransformer.buildMapFromPeerAndPreviousMap(peer, mPeerSloganMap);
                    mListAdapter.notifyPeerSloganListChanged(mPeerSloganMap);
                    reflectStatus();
                },
                (CommunicatorState state) -> {
                    mCommunicatorState = state;
                    reflectStatus();
                });

//        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        createListView();

        mMySloganManager.init();
        mListAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
        mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());

        ((SwipeRefreshLayout) findViewById(R.id.swiperefresh)).setOnRefreshListener(this::refresh);
    }

    /**
     * As the advertisement of peers are continuously monitored, triggering a refresh has zero impact.
     * As the user indicated a wish for an update, we briefly toast the current status.
     */
    void refresh() {
        i(TAG, "Showing swipe to refresh indicator to transport sense of immediacy");
        String text = mPeers.size() == 0
                ? getString(R.string.ui_main_toast_refresh_no_peers)
                : getResources().getQuantityString(R.plurals.ui_main_toast_refresh, mPeers.size(), mPeers.size());

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        mHandler.postDelayed(() -> ((SwipeRefreshLayout) findViewById(R.id.swiperefresh)).setRefreshing(false), SWIPE_TO_REFRESH_DURATION);
    }


    private void showAddDialog(View $) {
        if (!mMySloganManager.spaceAvailable()) {
            Toast.makeText(this, EmojiHelper.replaceShortCode(getString(R.string.ui_main_toast_cannot_add_no_space_available)), Toast.LENGTH_LONG).show();
            return;
        }
        mDialogManager.showParametrizedSloganEdit(R.string.ui_dialog_add_slogan_title,
                R.string.ui_dialog_add_slogan_text,
                R.string.ui_dialog_add_slogan_confirm,
                R.string.ui_dialog_add_slogan_cancel,
                null,
                (String sloganText) -> {
                    if (sloganText.length() == 0) {
                        Toast.makeText(this, R.string.ui_main_add_slogan_too_short, Toast.LENGTH_SHORT).show();
                    } else {
                        mMySloganManager.adopt(Slogan.create(sloganText));
                    }
                });
    }

    private void showEditDialog(Slogan slogan) {
        mDialogManager.showParametrizedSloganEdit(R.string.ui_dialog_edit_slogan_title,
                R.string.ui_dialog_edit_slogan_text,
                R.string.ui_dialog_edit_slogan_confirm,
                R.string.ui_dialog_edit_slogan_cancel,
                slogan,
                (String sloganText) -> mMySloganManager.replace(slogan, Slogan.create(sloganText)));
    }

    private void reflectStatus() {
        v(TAG, "Reflecting status, peers: %d, slogans: %d, state: %s", mPeers.size(), mPeerSloganMap.size(), mCommunicatorState);
        if (mCommunicatorState == null) {
            return;
        }
        if (!mCommunicatorState.mHasPermission) {
            sendToPermissionMissingActivity();
            return;
        }

        mStatusItem.mState = mCommunicatorState;
        mStatusItem.mPeers = mPeers;
        mStatusItem.mPeerSloganMap = mPeerSloganMap;
        mListAdapter.notifyListItemChanged(mStatusItem);

        mMySlogansHeadingItem.mMySlogansCount = mMySloganManager.getMySlogans().size();
        mListAdapter.notifyListItemChanged(mMySlogansHeadingItem);

        mPeersHeadingItem.mPeers = mPeers;
        mPeersHeadingItem.mSloganCount = mPeerSloganMap.size();
        mListAdapter.notifyListItemChanged(mPeersHeadingItem);

        if (mCommunicatorState.mRecentBtTurnOnEvents >= Communicator.RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD) {
            showBrokenBtStackAlert();
        }
    }

    private void showBrokenBtStackAlert() {
        if (!inForeground
                || System.currentTimeMillis() - mBrokenBtStackLastVisibleTimestamp > BROKEN_BT_STACK_ALERT_DEBOUNCE
                || mPrefs.getBoolean(MainActivity.PREFS_HIDE_BROKEN_BT_STACK_WARNING, false)) {
            return;
        }
        mDialogManager.showBtBroken(neverShowAgain -> {
            if (neverShowAgain) {
                mPrefs.edit().putBoolean(MainActivity.PREFS_HIDE_BROKEN_BT_STACK_WARNING, true).apply();
            } else {
                mBrokenBtStackLastVisibleTimestamp = System.currentTimeMillis();
            }
        });
    }

    private void createListView() {

        // TODO show stats for slogans
        RecyclerView listView = findViewById(R.id.list_view);

        List<ListItem> builtinItems = new ArrayList<>();

        mStatusItem = new StatusItem(mCommunicatorState, mPeers, mPeerSloganMap);
        builtinItems.add(mStatusItem);

        mMySlogansHeadingItem = new MySlogansHeadingItem(mMySloganManager.getMySlogans().size());
        builtinItems.add(mMySlogansHeadingItem);

        mPeersHeadingItem = new PeersHeadingItem(mPeers, mPeerSloganMap.size());
        builtinItems.add(mPeersHeadingItem);

        mListAdapter = new RecycleAdapter(this, builtinItems, listView);

        listView.setAdapter(mListAdapter);
        listView.setLayoutManager(new LinearLayoutManager(this));

        // With change animations enabled mStatusItem keeps flashing because updates come in
        ((SimpleItemAnimator) listView.getItemAnimator()).setSupportsChangeAnimations(false);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback(
                this,
                // The UI updated is delayed to give the dialog time to pop up in front of the resetting item
                () -> mHandler.postDelayed(mListAdapter::notifyDataSetChanged, 200),
                (Slogan slogan, int action) -> {

                    switch (action) {

                        case SwipeCallback.ACTION_ADOPT:
                            if (mMySloganManager.getMySlogans().contains(slogan)) {
                                Toast.makeText(this, R.string.ui_main_toast_slogan_already_adopted, Toast.LENGTH_LONG).show();
                            } else if (mMySloganManager.spaceAvailable()) {
                                mMySloganManager.adopt(slogan);
                            } else {
                                mDialogManager.showReplace(
                                        mMySloganManager.getMySlogans(),
                                        sloganToReplace -> mMySloganManager.replace(sloganToReplace, slogan)
                                );
                            }
                            break;

                        case SwipeCallback.ACTION_EDIT:
                            showEditDialog(slogan);
                            break;

                        case SwipeCallback.ACTION_DROP:
                            mDialogManager.showDrop(slogan, mMySloganManager::dropSlogan);
                            break;
                    }
                }));
        itemTouchHelper.attachToRecyclerView(listView);
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

    private void sendToPermissionMissingActivity() {
        Intent intent = new Intent(this, PermissionMissingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!PermissionHelper.granted(this)) {
            sendToPermissionMissingActivity();
            return;
        }

        if (mAuraEnabled) {
            mCommunicatorProxy.enable();
        }
        mCommunicatorProxy.startListening();
        mCommunicatorProxy.askForPeersUpdate();

        inForeground = true;

        mListAdapter.onResume();
    }

    @Override
    protected void onPause() {
        mCommunicatorProxy.stopListening();
        inForeground = false;

        mListAdapter.onPause();
        super.onPause();
    }
}
