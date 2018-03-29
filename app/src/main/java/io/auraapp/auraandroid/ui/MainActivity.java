package io.auraapp.auraandroid.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.Toast;

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
import io.auraapp.auraandroid.ui.debug.DebugFragment;
import io.auraapp.auraandroid.ui.permissions.PermissionsFragment;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;
import io.auraapp.auraandroid.ui.world.PeerMapTransformer;
import io.auraapp.auraandroid.ui.world.PeerSlogan;
import io.auraapp.auraandroid.ui.world.WorldFragment;
import io.auraapp.auraandroid.ui.world.list.RecycleAdapter;
import io.auraapp.auraandroid.ui.world.list.SwipeCallback;
import io.auraapp.auraandroid.ui.world.list.item.ListItem;
import io.auraapp.auraandroid.ui.world.list.item.PeersHeadingItem;
import io.auraapp.auraandroid.ui.world.list.item.StatusItem;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";

    private static final int BROKEN_BT_STACK_ALERT_DEBOUNCE = 1000 * 60;

    private RecycleAdapter mPeerListAdapter;
    private StatusItem mStatusItem;
    private PeersHeadingItem mPeersHeadingItem;
    private FakeSwipeRefreshLayout mSwipeRefresh;

    private MySloganManager mMySloganManager;
    private CommunicatorProxy mCommunicatorProxy;
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
    private DialogManager mDialogManager;

    private ScreenPager mPager;
    private ScreenPagerAdapter mPagerAdapter;
    private RecyclerView mPeerListView;
    private ToolbarAspect mToolbarAspect;
    private DebugFragment mDebugFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        setContentView(R.layout.activity_main);


        mPager = findViewById(R.id.pager);

        ViewGroup worldView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.world_fragment, findViewById(android.R.id.content), false);

        WorldFragment world = WorldFragment.create(worldView);

        mMySloganManager = new MySloganManager(this);

        mDialogManager = new DialogManager(this);

        mPagerAdapter = new ScreenPagerAdapter(
                getSupportFragmentManager(),
                ProfileFragment.create(
                        this,
                        mMySloganManager,
                        mDialogManager,
                        mOnSwipedCallback
                ),
                world,
                mPager,
                this);
        mPager.setAdapter(mPagerAdapter);

        // Load preferences
        mPrefs = getSharedPreferences(Prefs.PREFS_BUCKET, MODE_PRIVATE);

        if (!PermissionHelper.granted(this)) {
            showPermissionMissingFragment();
        } else {
            String currentScreen = mPrefs.getString(Prefs.PREFS_CURRENT_SCREEN, ScreenPagerAdapter.SCREEN_WELCOME);
            i(TAG, "Current screen is set to %s", currentScreen);
            mPager.goTo(mPagerAdapter.getClassForHandle(currentScreen), false);

            mPager.addChangeListener(fragment -> {
                mPrefs.edit()
                        .putString(
                                Prefs.PREFS_CURRENT_SCREEN,
                                mPagerAdapter.getHandleForClass(fragment.getClass()))
                        .apply();
            });
        }

        mMySloganManager.addChangedCallback(event -> {
            d(TAG, "My slogans changed");
            if (mToolbarAspect.isAuraEnabled()) {
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
        });
        mCommunicatorProxy = new CommunicatorProxy(
                this,
                peers -> {
                    mPeers = peers;
                    mSwipeRefresh.setPeerCount(mPeers.size());
                    mPeerSloganMap = PeerMapTransformer.buildMapFromPeerList(peers);
                    mPeerListAdapter.notifyPeerSloganListChanged(mPeerSloganMap);
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
                    mPeerListAdapter.notifyPeerSloganListChanged(mPeerSloganMap);
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

        mDebugFragment = DebugFragment.create(this);

        mSwipeRefresh = worldView.findViewById(R.id.swiperefresh);
        mSwipeRefresh.setEnabled(false);

        mPeerListView = worldView.findViewById(R.id.list_view);
        createListView();

        mMySloganManager.init();

        mToolbarAspect = new ToolbarAspect(
                this,
                mPager,
                mPrefs,
                mCommunicatorProxy,
                mMySloganManager,
                mHandler,
                mDebugFragment);
        mToolbarAspect.initToolbar();

        if (mToolbarAspect.isAuraEnabled()) {
            mCommunicatorProxy.enable();
        }

//        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        if (mToolbarAspect.isAuraEnabled()) {
            mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
        }

        reflectStatus();
    }

    // TODO peer adopts and drops slogan but stays visible here

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

        mStatusItem.mState = mCommunicatorState;
        mStatusItem.mPeers = mPeers;
        mStatusItem.mPeerSloganMap = mPeerSloganMap;
        mPeerListAdapter.notifyListItemChanged(mStatusItem);
        if (mToolbarAspect.isDebugFragmentEnabled()) {
            mDebugFragment.update(mCommunicatorState, mPeers);
        }

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
        mPeerListAdapter.notifyListItemChanged(mPeersHeadingItem);

        if (mCommunicatorState.mRecentBtTurnOnEvents >= Config.COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD) {
            showBrokenBtStackAlert();
        }

        mSwipeRefresh.setEnabled(mCommunicatorState.mScanning);
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

        mPeersHeadingItem = new PeersHeadingItem(mPeers, mPeerSloganMap.size());
        builtinItems.add(mPeersHeadingItem);

        mPeerListAdapter = new RecycleAdapter(this, builtinItems, mPeerListView);

        mPeerListView.setAdapter(mPeerListAdapter);
        mPeerListView.setLayoutManager(new LinearLayoutManager(this));

        // With change animations enabled mStatusItem keeps flashing because updates come in
        ((SimpleItemAnimator) mPeerListView.getItemAnimator()).setSupportsChangeAnimations(false);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback(
                this,
                // The UI updated is delayed to give the dialog time to pop up in front of the resetting item
                () -> mHandler.postDelayed(mPeerListAdapter::notifyDataSetChanged, 200),
                mOnSwipedCallback));
        itemTouchHelper.attachToRecyclerView(mPeerListView);
    }

    private SwipeCallback.OnSwipedCallback mOnSwipedCallback = (Slogan slogan, int action) -> {

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
    };

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
        if (mToolbarAspect.isAuraEnabled()) {
            mCommunicatorProxy.askForPeersUpdate();
        }

        inForeground = true;

        mPeerListAdapter.onResume();
    }

    @Override
    protected void onPause() {
        mCommunicatorProxy.stopListening();
        inForeground = false;

        mPeerListAdapter.onPause();
        super.onPause();
    }
}
