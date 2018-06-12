package io.auraapp.auraandroid.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.AuraPrefs;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.ExternalInvocation;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.ui.common.CommunicatorProxy;
import io.auraapp.auraandroid.ui.main.BrokenBtStackAlertFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;
import io.auraapp.auraandroid.ui.tutorial.TutorialManager;

import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_ADOPTED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_DROPPED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_NAME_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_REPLACED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_MY_PROFILE_TEXT_CHANGED_ACTION;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";

    private final Handler mHandler = new Handler();
    private SharedServicesSet mServicesSet;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOCAL_MY_PROFILE_ADOPTED_ACTION.equals(intent.getAction())) {
                toast(R.string.ui_profile_toast_slogan_adopted);

            } else if (LOCAL_MY_PROFILE_REPLACED_ACTION.equals(intent.getAction())) {
                toast(R.string.ui_profile_toast_slogan_replaced);

            } else if (LOCAL_MY_PROFILE_DROPPED_ACTION.equals(intent.getAction())) {
                toast(R.string.ui_profile_toast_slogan_dropped);

            } else if (LOCAL_MY_PROFILE_NAME_CHANGED_ACTION.equals(intent.getAction())) {
                toast(R.string.ui_profile_toast_name_changed);

            } else if (LOCAL_MY_PROFILE_TEXT_CHANGED_ACTION.equals(intent.getAction())) {
                toast(R.string.ui_profile_toast_text_changed);
            }
        }
    };


    // TODO Bug: peer adopts and drops slogan but stays visible here
    // TODO keep peers if aura disabled and communicator destroyed


    public SharedServicesSet getSharedServicesSet() {
        return mServicesSet;
    }

    /**
     * Body cannot be wrapped in mHandler.post() because then fragments crash because
     * they use e.g. mServicesSet
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Necessary for splash screen
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        AuraPrefs.init(this);

        setContentView(R.layout.main_activity);

        mServicesSet = new SharedServicesSet();
        mServicesSet.mMyProfileManager = new MyProfileManager(this);
        mServicesSet.mCommunicatorProxy = new CommunicatorProxy(this);
        mServicesSet.mDialogManager = new DialogManager(this);
        mServicesSet.mPager = findViewById(R.id.pager);

        mServicesSet.mTutorialManager = new TutorialManager(this, findViewById(R.id.tutorial_parent), mServicesSet.mPager);
        mServicesSet.mPager.setAdapter(new ScreenPagerAdapter(
                getSupportFragmentManager(),
                LocalBroadcastManager.getInstance(this)
        ));

//        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        mServicesSet.mCommunicatorProxy.updateMyProfile(mServicesSet.mMyProfileManager.getProfile());

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            //Same behavior as with "add" click
            //mServicesSet.mDialogManager.showParametrizedSloganEdit();
        }
    }

    private void toast(@StringRes int text) {
        Toast.makeText(this, EmojiHelper.replaceShortCode(getString(text)), Toast.LENGTH_SHORT).show();
    }

    @Override
    @ExternalInvocation
    protected void onResume() {
        super.onResume();

        if (!PermissionHelper.granted(this)) {
            mServicesSet.mPager.redirectIfNeeded(this, null);
            return;
        }

        mHandler.post(() -> {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    mReceiver,
                    IntentFactory.localMyProfileChangedIntentFiler()
            );

            mServicesSet.mCommunicatorProxy.resume();
            if (mServicesSet.mCommunicatorProxy.getState().mEnabled) {
                mServicesSet.mCommunicatorProxy.askForPeersUpdate();
            }

            // Make sure BrokenBtStackAlertFragment is instantiated and in place
            BrokenBtStackAlertFragment fragment = (BrokenBtStackAlertFragment) getSupportFragmentManager()
                    .findFragmentByTag("broken_bt_stack_alert");
            if (fragment == null) {
                getSupportFragmentManager().beginTransaction()
                        .add(new BrokenBtStackAlertFragment(), "broken_bt_stack_alert")
                        .commit();
            }
        });
    }

    @Override
    @ExternalInvocation
    protected void onPause() {
        super.onPause();
        mHandler.post(() -> {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
            mServicesSet.mCommunicatorProxy.pause();
        });
    }
}
