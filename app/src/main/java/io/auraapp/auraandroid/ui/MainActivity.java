package io.auraapp.auraandroid.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Prefs;
import io.auraapp.auraandroid.ui.common.CommunicatorProxy;
import io.auraapp.auraandroid.ui.common.MySloganManager;
import io.auraapp.auraandroid.ui.debug.DebugFragment;
import io.auraapp.auraandroid.ui.permissions.PermissionsFragment;
import io.auraapp.auraandroid.ui.profile.ProfileFragment;
import io.auraapp.auraandroid.ui.world.PeerMapTransformer;
import io.auraapp.auraandroid.ui.world.PeerSlogan;
import io.auraapp.auraandroid.ui.world.WorldFragment;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";

    private static final int BROKEN_BT_STACK_ALERT_DEBOUNCE = 1000 * 60;

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
    private ToolbarAspect mToolbarAspect;
    private DebugFragment mDebugFragment;
    private WorldFragment mWorldFragment;

    @Override
    @ExternalInvocation
    protected void onCreate(Bundle savedInstanceState) {

        // Necessary for splash screen
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);

        super.onCreate(savedInstanceState);

        mHandler.post(() -> {
            v(TAG, "onCreate, intent: %s", getIntent().getAction());

            setContentView(R.layout.activity_main);

            mPager = findViewById(R.id.pager);

            mWorldFragment = WorldFragment.create(
                    this,
                    slogan -> {
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
                    });

            mMySloganManager = new MySloganManager(this);
            mDialogManager = new DialogManager(this);

            mPagerAdapter = new ScreenPagerAdapter(
                    getSupportFragmentManager(),
                    ProfileFragment.create(this, mMySloganManager, mDialogManager),
                    mWorldFragment,
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
                        mPeerSloganMap = PeerMapTransformer.buildMapFromPeerList(peers);
                        mWorldFragment.mPeerListAdapter.notifyPeerSloganListChanged(mPeerSloganMap);
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
                        mWorldFragment.mPeerListAdapter.notifyPeerSloganListChanged(mPeerSloganMap);
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
        });
    }

    // TODO peer adopts and drops slogan but stays visible here

    private void reflectStatus() {
        v(TAG, "Reflecting status, peers: %d, slogans: %d, state: %s", mPeers.size(), mPeerSloganMap.size(), mCommunicatorState);

        if (mToolbarAspect.isDebugFragmentEnabled()) {
            mDebugFragment.update(mCommunicatorState, mPeers);
        }

        mWorldFragment.update(
                mCommunicatorState,
                mPeerSloganMap,
                mPeers);

        if (mCommunicatorState == null) {
            return;
        }
        if (!mCommunicatorState.mHasPermission) {
            showPermissionMissingFragment();
            return;
        }

        // TODO keep peers if aura disabled and communicator destroyed


        if (mCommunicatorState.mRecentBtTurnOnEvents >= Config.COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD) {
            showBrokenBtStackAlert();
        }
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

    @FunctionalInterface
    interface ToolbarButtonVisibilityUpdater {
        void update(Fragment fragment);
    }

    /**
     * This is called once, after onCreate
     * https://stackoverflow.com/questions/7705927/android-when-is-oncreateoptionsmenu-called-during-activity-lifecycle
     */
    @Override
    @ExternalInvocation
    public boolean onCreateOptionsMenu(Menu menu) {
        mHandler.post(() -> mToolbarAspect.createOptionsMenu(menu));
        return true;
    }

    private void showPermissionMissingFragment() {
        mPagerAdapter.addPermissionsFragment();
        mPager.goTo(PermissionsFragment.class, false);
    }

    @Override
    @ExternalInvocation
    protected void onResume() {
        super.onResume();

        mHandler.post(() -> {
            if (!PermissionHelper.granted(this)) {
                showPermissionMissingFragment();
                return;
            }

            mCommunicatorProxy.startListening();
            if (mToolbarAspect.isAuraEnabled()) {
                mCommunicatorProxy.askForPeersUpdate();
            }

            inForeground = true;
            mWorldFragment.onResume();
        });
    }

    @Override
    @ExternalInvocation
    protected void onPause() {
        super.onPause();

        mHandler.post(() -> {
            mCommunicatorProxy.stopListening();
            inForeground = false;

            mWorldFragment.onPause();
        });
    }
}
