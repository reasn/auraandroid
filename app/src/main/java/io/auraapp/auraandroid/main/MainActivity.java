package io.auraapp.auraandroid.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
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
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.PermissionMissingActivity;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Prefs;
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

    private static final int BROKEN_BT_STACK_ALERT_DEBOUNCE = 1000 * 60;
    private static final long SWIPE_TO_REFRESH_DURATION = 1000 * 2;

    private RecycleAdapter mListAdapter;
    private StatusItem mStatusItem;
    private PeersHeadingItem mPeersHeadingItem;
    private SwipeRefreshLayout mSwipeRefresh;

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
    private ScrollView mDebugWrapper;
    private TextView mDebugCommunicatorStateDumpView;
    private ListView mDebugPeersListView;

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
        mPrefs = getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE);
        mAuraEnabled = mPrefs.getBoolean(Prefs.PREFS_ENABLED, true);

        mDialogManager = new DialogManager(this);

        final FloatingActionButton addSloganButton = findViewById(R.id.add_slogan);
        addSloganButton.setOnClickListener($ -> showAddDialog());

        mMySloganManager = new MySloganManager(
                this,
                event -> {
                    d(TAG, "My slogans changed");
                    mListAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
                    if (mAuraEnabled) {
                        mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
                    }
                    switch (event) {
                        case MySloganManager.EVENT_ADOPTED:
                            toast(R.string.ui_main_toast_adopted);
                            break;
                        case MySloganManager.EVENT_REPLACED:
                            toast(R.string.ui_main_toast_replaced);
                            break;
                        case MySloganManager.EVENT_DROPPED:
                            toast(R.string.ui_main_toast_dropped);
                            break;
                        default:
                            throw new RuntimeException("Unknown slogan event " + event);
                    }
                    reflectStatus();
                }
        );
        mCommunicatorProxy = new CommunicatorProxy(
                this,
                peers -> {
                    mPeers = peers;
                    mPeerSloganMap = PeerMapTransformer.buildMapFromPeerList(peers);
                    mListAdapter.notifyPeerSloganListChanged(mPeerSloganMap);
                    reflectStatus();
                },
                peer -> {
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
                state -> {
                    if (mCommunicatorState == null || !mCommunicatorState.mScanning && state.mScanning) {
                        // Scan just started, let's make sure we hide the "looking around" info if
                        // nothing is found for some time.
                        mHandler.postDelayed(this::reflectStatus, Config.MAIN_LOOKING_AROUND_SHOW_DURATION);
                    }
                    mCommunicatorState = state;
                    reflectStatus();
                });

        if (mAuraEnabled) {
            mCommunicatorProxy.enable();
        }

//        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        createListView();

        mMySloganManager.init();
        mListAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
        if (mAuraEnabled) {
            mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
        }

        mSwipeRefresh = findViewById(R.id.swiperefresh);
        mSwipeRefresh.setOnRefreshListener(this::refresh);
        mSwipeRefresh.setEnabled(false);

        mDebugWrapper = findViewById(R.id.debug_wrapper);
        mDebugCommunicatorStateDumpView = findViewById(R.id.debug_communicator_state_dump);
        mDebugPeersListView = findViewById(R.id.debug_peers_list);
        reflectStatus();
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

        toast(text);
        mHandler.postDelayed(() -> mSwipeRefresh.setRefreshing(false), SWIPE_TO_REFRESH_DURATION);
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

    private void showEditDialog(Slogan slogan) {
        mDialogManager.showParametrizedSloganEdit(R.string.ui_dialog_edit_slogan_title,
                R.string.ui_dialog_edit_slogan_text,
                R.string.ui_dialog_edit_slogan_confirm,
                R.string.ui_dialog_edit_slogan_cancel,
                slogan,
                sloganText -> mMySloganManager.replace(slogan, Slogan.create(sloganText)));
    }

    private void reflectStatus() {
        v(TAG, "Reflecting status, peers: %d, slogans: %d, state: %s", mPeers.size(), mPeerSloganMap.size(), mCommunicatorState);

        mMySlogansHeadingItem.mMySlogansCount = mMySloganManager.getMySlogans().size();
        mListAdapter.notifyListItemChanged(mMySlogansHeadingItem);

        mStatusItem.mState = mCommunicatorState;
        mStatusItem.mPeers = mPeers;
        mStatusItem.mPeerSloganMap = mPeerSloganMap;
        mListAdapter.notifyListItemChanged(mStatusItem);
        showDebugInformation();

        if (mCommunicatorState == null) {
            return;
        }
        if (!mCommunicatorState.mHasPermission) {
            sendToPermissionMissingActivity();
            return;
        }

        // TODO keep peers if aura disabled and communicator destroyed
        mPeersHeadingItem.mPeers = mPeers;
        mPeersHeadingItem.mSloganCount = mPeerSloganMap.size();
        mPeersHeadingItem.mScanning = mCommunicatorState.mScanning;
        mPeersHeadingItem.mScanStartTimestamp = mCommunicatorState.mScanStartTimestamp;
        mListAdapter.notifyListItemChanged(mPeersHeadingItem);

        if (mCommunicatorState.mRecentBtTurnOnEvents >= Config.COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD) {
            showBrokenBtStackAlert();
        }

        mSwipeRefresh.setEnabled(mCommunicatorState.mScanning);
    }

    private void showDebugInformation() {
        mDebugWrapper.setVisibility(View.GONE);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String dump = "# communicator: " + gson.toJson(mCommunicatorState);
        dump += "\n# peers: " + gson.toJson(mPeers);
        mDebugCommunicatorStateDumpView.setText(dump.replaceAll("\"", "").replaceAll("\n +\\{", " {"));

        // TODO don't recreate everything, cache? maybe not #onlyfordebugging
        mDebugPeersListView.setAdapter(new DebugPeersListArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                mPeers.toArray(new Peer[mPeers.size()])
        ));
    }

    private void showBrokenBtStackAlert() {
        if (!inForeground
                || System.currentTimeMillis() - mBrokenBtStackLastVisibleTimestamp > BROKEN_BT_STACK_ALERT_DEBOUNCE
                || mPrefs.getBoolean(Prefs.PREFS_HIDE_BROKEN_BT_STACK_WARNING, false)) {
            return;
        }
        mDialogManager.showBtBroken(neverShowAgain -> {
            if (neverShowAgain) {
                mPrefs.edit().putBoolean(Prefs.PREFS_HIDE_BROKEN_BT_STACK_WARNING, true).apply();
            } else {
                mBrokenBtStackLastVisibleTimestamp = System.currentTimeMillis();
            }
        });
    }

    private void toast(@StringRes int text) {
        Toast.makeText(this, EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
    }

    private void toast(String text) {
        Toast.makeText(this, EmojiHelper.replaceShortCode(text), Toast.LENGTH_SHORT).show();
    }

    private void createListView() {

        // TODO show stats for slogans
        RecyclerView listView = findViewById(R.id.list_view);

        List<ListItem> builtinItems = new ArrayList<>();

        mStatusItem = new StatusItem(mCommunicatorState, mPeers, mPeerSloganMap);
        builtinItems.add(mStatusItem);

        mMySlogansHeadingItem = new MySlogansHeadingItem(
                mMySloganManager.getMySlogans().size(),
                this::showAddDialog);
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
                                toast(R.string.ui_main_toast_slogan_already_adopted);
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

        SwitchCompat enabledSwitch = item.getActionView().findViewById(R.id.enabled_switch);
        enabledSwitch.setChecked(mAuraEnabled);

        // Managed programmatically because offText XML attribute has no effect for SwitchCompat in menu item
        enabledSwitch.setText(getString(mAuraEnabled
                ? R.string.ui_toolbar_enable_on
                : R.string.ui_toolbar_enable_off));

        enabledSwitch.setOnCheckedChangeListener((CompoundButton $, boolean isChecked) -> {
            mAuraEnabled = isChecked;
            mPrefs.edit().putBoolean(Prefs.PREFS_ENABLED, isChecked).apply();
            if (isChecked) {
                mCommunicatorProxy.enable();
                mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
                mCommunicatorProxy.askForPeersUpdate();
                enabledSwitch.setText(getString(R.string.ui_toolbar_enable_on));
                enabledSwitch.getThumbDrawable().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
                enabledSwitch.getTrackDrawable().setColorFilter(getResources().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);

            } else {
                enabledSwitch.setText(getString(R.string.ui_toolbar_enable_off));
                mCommunicatorProxy.disable();
                enabledSwitch.getThumbDrawable().setColorFilter(getResources().getColor(R.color.red), PorterDuff.Mode.MULTIPLY);
                enabledSwitch.getTrackDrawable().setColorFilter(getResources().getColor(R.color.red), PorterDuff.Mode.MULTIPLY);
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

        mCommunicatorProxy.startListening();
        if (mAuraEnabled) {
            mCommunicatorProxy.askForPeersUpdate();
        }

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
