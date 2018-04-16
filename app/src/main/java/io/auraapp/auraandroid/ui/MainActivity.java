package io.auraapp.auraandroid.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.Toast;

import java.util.TreeMap;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.ui.common.CommunicatorProxy;
import io.auraapp.auraandroid.ui.permissions.PermissionsFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;
import io.auraapp.auraandroid.ui.tutorial.SloganAddStep;
import io.auraapp.auraandroid.ui.tutorial.TutorialManager;
import io.auraapp.auraandroid.ui.welcome.TermsFragment;
import io.auraapp.auraandroid.ui.world.PeerMapTransformer;
import io.auraapp.auraandroid.ui.world.PeerSlogan;

import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";
    private static final int BROKEN_BT_STACK_ALERT_DEBOUNCE = 1000 * 60;

    private MyProfileManager mMyProfileManager;
    private CommunicatorProxy mCommunicatorProxy;
    private boolean inForeground = false;
    private SharedPreferences mPrefs;
    private long mBrokenBtStackLastVisibleTimestamp;
    private final Handler mHandler = new Handler();
    /**
     * slogan:PeerSlogan
     */
    TreeMap<String, PeerSlogan> mPeerSloganMap = new TreeMap<>();

    private ScreenPagerAdapter mPagerAdapter;
    private ToolbarAspect mToolbarAspect;
    private SharedServicesSet mSharedServicesSet;
    private SharedState mState;

    public SharedServicesSet getSharedServicesSet() {
        return mSharedServicesSet;
    }

    public SharedState getSharedState() {
        return mState;
    }

    /**
     * Body cannot be wrapped in mHandler.post() because then fragments crash because they use
     * e.g. SharedState mState.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Necessary for splash screen
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);

        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        setContentView(R.layout.activity_main);

        mMyProfileManager = new MyProfileManager(this);

        mState = new SharedState();

        mSharedServicesSet = new SharedServicesSet();
        mSharedServicesSet.mMyProfileManager = mMyProfileManager;
        mSharedServicesSet.mDialogManager = new DialogManager(this);
        mSharedServicesSet.mPager = findViewById(R.id.pager);

        mSharedServicesSet.mTutorialManager = new TutorialManager(this, findViewById(R.id.activity_wrapper), mSharedServicesSet.mPager);

        mPagerAdapter = new ScreenPagerAdapter(getSupportFragmentManager());
        mSharedServicesSet.mPager.setAdapter(mPagerAdapter);

        // Load preferences
        mPrefs = getSharedPreferences(Config.PREFERENCES_BUCKET, MODE_PRIVATE);

        String prefKey = getString(R.string.prefs_terms_agreed);

        if (!PermissionHelper.granted(this)) {
            showPermissionMissingFragment();
        } else if (!mPrefs.getBoolean(prefKey, false)) {
            mSharedServicesSet.mPager.getScreenAdapter().addWelcomeFragments();
            mSharedServicesSet.mPager.goTo(TermsFragment.class, false);
        }
        mMyProfileManager.addChangedCallback(event -> {
            d(TAG, "My profile changed");
            mCommunicatorProxy.updateMyProfile(mMyProfileManager.getProfile());
            switch (event) {
                case MyProfileManager.EVENT_ADOPTED:
                    toast(R.string.ui_profile_toast_slogan_adopted);
                    break;
                case MyProfileManager.EVENT_REPLACED:
                    toast(R.string.ui_profile_toast_slogan_replaced);
                    break;
                case MyProfileManager.EVENT_DROPPED:
                    toast(R.string.ui_profile_toast_slogan_dropped);
                    break;
                case MyProfileManager.EVENT_COLOR_CHANGED:
                    break;
                case MyProfileManager.EVENT_NAME_CHANGED:
                    toast(R.string.ui_profile_toast_name_changed);
                    break;
                case MyProfileManager.EVENT_TEXT_CHANGED:
                    toast(R.string.ui_profile_toast_text_changed);
                    break;
                default:
                    throw new RuntimeException("Unknown slogan event " + event);
            }
        });
        mCommunicatorProxy = new CommunicatorProxy(
                this,
                peers -> {
                    mSharedServicesSet.mPeers = peers;
                    mPeerSloganMap = PeerMapTransformer.buildMapFromPeerList(peers);
                },
                peer -> {
                    for (Peer candidate : mSharedServicesSet.mPeers) {
                        if (candidate.mId == peer.mId) {
                            mSharedServicesSet.mPeers.remove(candidate);
                            break;
                        }
                    }
                    mSharedServicesSet.mPeers.add(peer);
                    mPeerSloganMap = PeerMapTransformer.buildMapFromPeerAndPreviousMap(peer, mPeerSloganMap);
                },
                state -> {
                    d(TAG, "Received communicator state, state: %s", state);
//                        TODO add to world fragment
//                        if (mState.mCommunicatorState == null || !mState.mCommunicatorState.mScanning && state.mScanning) {
//                            // Scan just started, let's make sure we hide the "looking around" info if
//                            // nothing is found for some time.
//                            mHandler.postDelayed(this::reflectStatus, Config.MAIN_LOOKING_AROUND_SHOW_DURATION);
//                        }
                    mState.mCommunicatorState = state;

                    if (state != null && !state.mHasPermission) {
                        showPermissionMissingFragment();
                        return;
                    }
                    if (state != null && state.mRecentBtTurnOnEvents >= Config.COMMUNICATOR_RECENT_BT_TURNING_ON_EVENTS_ALERT_THRESHOLD) {
                        showBrokenBtStackAlert();
                    }
                });

        mToolbarAspect = new ToolbarAspect(this, mCommunicatorProxy, mHandler);
        mToolbarAspect.initToolbar();

        if (mToolbarAspect.isAuraEnabled()) {
            // Will result in state being sent back
            mCommunicatorProxy.enable();
        }

//        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        mCommunicatorProxy.updateMyProfile(mMyProfileManager.getProfile());
    }

    // TODO Bug: peer adopts and drops slogan but stays visible here

    // TODO keep peers if aura disabled and communicator destroyed

    private void showBrokenBtStackAlert() {
        String prefKey = getString(R.string.prefs_hide_broken_bt_warning_key);
        if (!inForeground
                || System.currentTimeMillis() - mBrokenBtStackLastVisibleTimestamp > BROKEN_BT_STACK_ALERT_DEBOUNCE
                || mPrefs.getBoolean(prefKey, false)) {
            return;
        }
        mSharedServicesSet.mDialogManager.showBtBroken(neverShowAgain -> {
            if (neverShowAgain) {
                mPrefs.edit().putBoolean(prefKey, true).apply();
            } else {
                mBrokenBtStackLastVisibleTimestamp = System.currentTimeMillis();
            }
        });
    }

    private void toast(@StringRes int text) {
        Toast.makeText(this, EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
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
        mSharedServicesSet.mPager.goTo(PermissionsFragment.class, false);
    }

    @Override
    @ExternalInvocation
    protected void onResume() {
        super.onResume();
        if (!PermissionHelper.granted(this)) {
            showPermissionMissingFragment();
            return;
        }

        mHandler.post(() -> {
            mCommunicatorProxy.startListening();
            if (mToolbarAspect.isAuraEnabled()) {
                mCommunicatorProxy.askForPeersUpdate();
            }
            inForeground = true;
        });
    }

    @Override
    @ExternalInvocation
    protected void onPause() {
        super.onPause();

        mHandler.post(() -> {
            mCommunicatorProxy.stopListening();
            inForeground = false;
        });
    }


    public void showTutorial() {
        mSharedServicesSet.mTutorialManager.goTo(SloganAddStep.class);
    }

}
