package io.auraapp.auraandroid.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
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
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Prefs;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.ui.common.CommunicatorProxy;
import io.auraapp.auraandroid.ui.common.MySloganManager;
import io.auraapp.auraandroid.ui.debug.DebugPeersListArrayAdapter;
import io.auraapp.auraandroid.ui.permissions.PermissionsFragment;
import io.auraapp.auraandroid.ui.world.PeerMapTransformer;
import io.auraapp.auraandroid.ui.world.PeerSlogan;
import io.auraapp.auraandroid.ui.world.WorldFragment;
import io.auraapp.auraandroid.ui.world.list.RecycleAdapter;
import io.auraapp.auraandroid.ui.world.list.SwipeCallback;
import io.auraapp.auraandroid.ui.world.list.item.ListItem;
import io.auraapp.auraandroid.ui.world.list.item.MySlogansHeadingItem;
import io.auraapp.auraandroid.ui.world.list.item.PeersHeadingItem;
import io.auraapp.auraandroid.ui.world.list.item.StatusItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";

    private static final int BROKEN_BT_STACK_ALERT_DEBOUNCE = 1000 * 60;

    private RecycleAdapter mListAdapter;
    private StatusItem mStatusItem;
    private PeersHeadingItem mPeersHeadingItem;
    private FakeSwipeRefreshLayout mSwipeRefresh;

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

    private ScreenPager mPager;
    private ScreenPagerAdapter mPagerAdapter;
    private RecyclerView mListView;
    private ToolbarAspect mToolbarAspect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        setContentView(R.layout.activity_main);


        mPager = findViewById(R.id.pager);

        ViewGroup worldView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.fragment_screen_world, findViewById(android.R.id.content), false);

        WorldFragment world = WorldFragment.create(worldView);

        mPagerAdapter = new ScreenPagerAdapter(getSupportFragmentManager(), world, mPager, this);
        mPager.setAdapter(mPagerAdapter);

        // Load preferences
        mPrefs = getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE);
        mAuraEnabled = mPrefs.getBoolean(Prefs.PREFS_ENABLED, true);

        if (!PermissionHelper.granted(this)) {
            showPermissionMissingFragment();
        } else {
            String currentScreen = mPrefs.getString(Prefs.PREFS_CURRENT_SCREEN, ScreenPagerAdapter.SCREEN_WELCOME);
            mPager.goTo(mPagerAdapter.getClassForHandle(currentScreen), false);

            mPager.addChangeListener(fragment -> {
                mPrefs.edit()
                        .putString(
                                Prefs.PREFS_CURRENT_SCREEN,
                                mPagerAdapter.getHandleForClass(fragment.getClass()))
                        .apply();
            });
        }

        mDialogManager = new DialogManager(this);

        final FloatingActionButton addSloganButton = worldView.findViewById(R.id.add_slogan);
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
                    mSwipeRefresh.setPeerCount(mPeers.size());
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

        mListView = worldView.findViewById(R.id.list_view);
        createListView();

        mMySloganManager.init();
        mListAdapter.notifyMySlogansChanged(mMySloganManager.getMySlogans());
        if (mAuraEnabled) {
            mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
        }

        mSwipeRefresh = worldView.findViewById(R.id.swiperefresh);
        mSwipeRefresh.setEnabled(false);

        mToolbarAspect = new ToolbarAspect(
                this,
                mPager,
                mPrefs,
                mCommunicatorProxy,
                mMySloganManager,
                mHandler
        );
        mToolbarAspect.initToolbar();

        mDebugWrapper = worldView.findViewById(R.id.debug_wrapper);
        mDebugCommunicatorStateDumpView = worldView.findViewById(R.id.debug_communicator_state_dump);
        mDebugPeersListView = worldView.findViewById(R.id.debug_peers_list);
        reflectStatus();
    }

    // TODO peer adopts and drops slogan but stays visible here
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
        updateDebugView();

        if (mCommunicatorState == null) {
            return;
        }
        if (!mCommunicatorState.mHasPermission) {
            showPermissionMissingFragment();
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

    private void updateDebugView() {
        if (mDebugWrapper.getVisibility() == View.GONE) {
            return;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String dump = "# communicator: " + gson.toJson(mCommunicatorState);
        dump += "\n# peers: " + gson.toJson(mPeers);
        mDebugCommunicatorStateDumpView.setText(dump.replaceAll("\"", "").replaceAll("\n +\\{", " {"));

        // Not efficient but minimal code. That's okay because it doesn't affect performance
        // for users
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

        List<ListItem> builtinItems = new ArrayList<>();

        mStatusItem = new StatusItem(mCommunicatorState, mPeers, mPeerSloganMap);
        builtinItems.add(mStatusItem);

        mMySlogansHeadingItem = new MySlogansHeadingItem(
                mMySloganManager.getMySlogans().size(),
                this::showAddDialog);
        builtinItems.add(mMySlogansHeadingItem);

        mPeersHeadingItem = new PeersHeadingItem(mPeers, mPeerSloganMap.size());
        builtinItems.add(mPeersHeadingItem);

        mListAdapter = new RecycleAdapter(this, builtinItems, mListView);

        mListView.setAdapter(mListAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(this));

        // With change animations enabled mStatusItem keeps flashing because updates come in
        ((SimpleItemAnimator) mListView.getItemAnimator()).setSupportsChangeAnimations(false);

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
        itemTouchHelper.attachToRecyclerView(mListView);
    }


    @FunctionalInterface
    interface ToolbarButtonVisibilityUpdater {
        void update(Fragment fragment);
    }

    /**
     * This is called once, after onCreate
     * https://stackoverflow.com/questions/7705927/android-when-is-oncreateoptionsmenu-called-during-activity-lifecycle
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mHandler.post(() -> mToolbarAspect.createOptionsMenu(menu));
        return true;
    }

    private void showPermissionMissingFragment() {
        mPagerAdapter.addPermissionsFragment();
        mPager.goTo(PermissionsFragment.class, false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!PermissionHelper.granted(this)) {
            showPermissionMissingFragment();
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
