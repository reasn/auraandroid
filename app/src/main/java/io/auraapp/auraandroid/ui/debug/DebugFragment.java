package io.auraapp.auraandroid.ui.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashSet;
import java.util.Set;

import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Config;
import io.auraapp.auraandroid.common.EmojiHelper;
import io.auraapp.auraandroid.common.IntentFactory;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.Timer;
import io.auraapp.auraandroid.ui.MainActivity;
import io.auraapp.auraandroid.ui.SharedServicesSet;
import io.auraapp.auraandroid.ui.common.ColorPicker;
import io.auraapp.auraandroid.ui.common.CommunicatorProxyState;
import io.auraapp.auraandroid.ui.common.fragments.ContextViewFragment;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfile;
import io.auraapp.auraandroid.ui.profile.profileModel.MyProfileManager;

import static android.content.Context.MODE_PRIVATE;
import static io.auraapp.auraandroid.common.FormattedLog.i;
import static io.auraapp.auraandroid.common.FormattedLog.v;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_LIST_UPDATED_EXTRA_PEERS;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_ACTION;
import static io.auraapp.auraandroid.common.IntentFactory.INTENT_PEER_UPDATED_EXTRA_PEER;
import static io.auraapp.auraandroid.common.IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION;
import static io.auraapp.auraandroid.ui.common.CommunicatorProxy.replacePeer;

public class DebugFragment extends ContextViewFragment {

    private static final String TAG = "@aura/ui/permissions/" + DebugFragment.class.getSimpleName();
    private static final String characters = "📜📡💚😇abcdefghijklmnopqrstuvwxyz1234567890 ,.-öä#ü+!\"§$%&/()=?`";
    private final Handler mHandler = new Handler();
    private final Timer mTimer = new Timer(mHandler);
    private Timer.Timeout mRefreshTimeout;
    @Nullable
    private Set<Peer> mPeers;
    private CommunicatorProxyState mState;
    private MyProfile mProfile;
    private long mLastStateUpdateTimestamp;

    private long mLastIntentTimestamp;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.post(() -> {
                v(TAG, "onReceive, intent: %s", intent.getAction());
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }

                mLastIntentTimestamp = System.currentTimeMillis();

                if (LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    mState = (CommunicatorProxyState) extras.getSerializable(IntentFactory.LOCAL_COMMUNICATOR_STATE_CHANGED_EXTRA_PROXY_STATE);
                    mLastStateUpdateTimestamp = System.currentTimeMillis();
                }
                if (INTENT_PEER_UPDATED_ACTION.equals(intent.getAction())) {
                    @SuppressWarnings("unchecked")
                    Peer peer = (Peer) extras.getSerializable(INTENT_PEER_UPDATED_EXTRA_PEER);
                    if (peer != null) {
                        if (mPeers == null) {
                            mPeers = new HashSet<>();
                        }
                        replacePeer(mPeers, peer);
                    }

                }
                if (INTENT_PEER_LIST_UPDATED_ACTION.equals(intent.getAction())) {
                    @SuppressWarnings("unchecked")
                    Set<Peer> peers = (Set<Peer>) extras.getSerializable(INTENT_PEER_LIST_UPDATED_EXTRA_PEERS);
                    if (peers != null) {
                        mPeers = peers;
                    }
                }
                reflectState(context);
            });
        }
    };

    private String createRandomStringOfLength(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt((int) (Math.random() * characters.length())));
        }
        return result.toString();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.debug_fragment;
    }

    @Override
    protected void onResumeWithContextAndView(MainActivity activity, ViewGroup rootView) {
        LocalBroadcastManager.getInstance(activity).registerReceiver(mReceiver, IntentFactory.createFilter(
                LOCAL_COMMUNICATOR_STATE_CHANGED_ACTION,
                INTENT_PEER_LIST_UPDATED_ACTION,
                INTENT_PEER_UPDATED_ACTION));
        v(TAG, "Receivers registered");

        SharedServicesSet servicesSet = activity.getSharedServicesSet();
        MyProfileManager profileManager = servicesSet.mMyProfileManager;
        mState = servicesSet.mCommunicatorProxy.getState();
        mPeers = servicesSet.mCommunicatorProxy.getPeers();
        mProfile = servicesSet.mMyProfileManager.getProfile();
        mLastStateUpdateTimestamp = System.currentTimeMillis();

        rootView.findViewById(R.id.demo_edge).setOnClickListener($ -> mHandler.post(() -> {
            profileManager.setName(createRandomStringOfLength(Config.PROFILE_NAME_MAX_LENGTH));

            StringBuilder text = new StringBuilder();
            profileManager.setColor(new ColorPicker.SelectedColor("#ff00ff", 0, 0));
            for (int i = 0; i < Config.PROFILE_TEXT_MAX_LINE_BREAKS; i++) {
                text.append(createRandomStringOfLength(Config.PROFILE_TEXT_MAX_LENGTH / Config.PROFILE_TEXT_MAX_LINE_BREAKS)).append("\n");
            }
            profileManager.setText(text.toString());
            profileManager.dropAllSlogans();
            for (int i = 0; i < Config.PROFILE_SLOGANS_MAX_SLOGANS; i++) {
                profileManager.adopt(Slogan.create(createRandomStringOfLength(Config.PROFILE_SLOGANS_MAX_LENGTH)));
            }
        }));
        rootView.findViewById(R.id.demo_anonymous).setOnClickListener($ -> mHandler.post(() -> {
            profileManager.setColor(new ColorPicker.SelectedColor("#000000", 0, 0));
            profileManager.setName("Anonymous");
            profileManager.setText(EmojiHelper.replaceShortCode(":fire::fire::fire:\nDemocracy prevails. Let your kindness be a symbol for humanism and a better future"));
            profileManager.dropAllSlogans();
            profileManager.adopt(Slogan.create("🕯 Free proxies: 96.12.58.120 and 96.12.82.10."));
            profileManager.adopt(Slogan.create("😷😭 Teargas! Bring water and masks"));
            profileManager.adopt(Slogan.create("Democracy Now!"));
        }));

        rootView.findViewById(R.id.demo_jen).setOnClickListener($ -> mHandler.post(() -> {
            profileManager.setColor(new ColorPicker.SelectedColor("#ffffff", 0, 0));
            profileManager.setName("Jen Bendson");
            profileManager.setText("I'm giving a talk on Friday, 2pm:"
                    + "\n\"Positive effects of health and happiness\""
                    + "\n\nFind me!"
                    + "\n@jen.benson"
                    + "\nLinkedIn.com/in/jenbenson");
            profileManager.dropAllSlogans();
            profileManager.adopt(Slogan.create("#SugarKills"));
            profileManager.adopt(Slogan.create("4pm: Q&A @ speakers corner"));
        }));

        rootView.findViewById(R.id.demo_clara).setOnClickListener($ -> mHandler.post(() -> {
            profileManager.setColor(new ColorPicker.SelectedColor("#00ff00", 0, 0));
            profileManager.setName("Clara");
            profileManager.setText(EmojiHelper.replaceShortCode("My brother wants to collect"));
            profileManager.dropAllSlogans();
            profileManager.adopt(Slogan.create("Hello Moto"));
        }));

        rootView.findViewById(R.id.demo_alex).setOnClickListener($ -> mHandler.post(() -> {
            profileManager.setColor(new ColorPicker.SelectedColor("#ff5f08", 0, 0));
            profileManager.setName("Alexander");
            profileManager.setText("Hey early adopting super hero 🤖"
                    + "\n\nI created Aura and need you!"
                    + "\nPlease help at getaura.io/feedback"
                    + "\n\n@alexanderthiel"
                    + "\nLinkedIn.com/in/reasn");
            profileManager.dropAllSlogans();
            profileManager.adopt(Slogan.create("Aura goes IoT!\nHelp at getaura.io/iot"));
        }));

        rootView.findViewById(R.id.debug_log_dump).setOnClickListener($ -> {
            i(TAG, createDump(activity));
            toast(R.string.debug_dump_logged);
        });

        Timer.clear(mRefreshTimeout);
        mRefreshTimeout = mTimer.setSerializedInterval(() -> mHandler.post(() -> reflectState(activity)), 1000);
        reflectState(activity);
    }

    @Override
    protected void onPauseWithContext(MainActivity activity) {
        super.onPauseWithContext(activity);
        Timer.clear(mRefreshTimeout);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mReceiver);
        v(TAG, "Receivers unregistered");
    }

    private void reflectState(Context context) {

        String dump = createDump(context);
        TextView communicatorStateDump = getRootView().findViewById(R.id.debug_communicator_state_dump);
        communicatorStateDump.setText(dump.replaceAll("\"", "").replaceAll("\n +\\{", " {"));
    }

    private String createDump(Context context) {
        long now = System.currentTimeMillis();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String dump = "\nlast communicator intent: " +
                (mLastIntentTimestamp > 0
                        ? (now - mLastIntentTimestamp) / 1000 + "s ago"
                        : "never");
        dump += "\nlast communicator state: " + (now - mLastStateUpdateTimestamp) / 1000 + "s ago";
        dump += "\nprofile: " + gson.toJson(mProfile);
        dump += "\ncommunicator: " + gson.toJson(mState);
        dump += "\npeers: " + gson.toJson(mPeers);
        dump += createPrefsDump(context);
        return dump;
    }

    private String renderBooleanPref(SharedPreferences prefs, Context context, @StringRes int key) {
        return prefs.contains(context.getString(key))
                ? (
                prefs.getBoolean(context.getString(key), false)
                        ? "true"
                        : "false")
                : "not set";
    }

    private String renderStringPref(SharedPreferences prefs, Context context, @StringRes int key) {
        return prefs.contains(context.getString(key))
                ? prefs.getString(context.getString(key), "")
                : "not set";
    }

    private String createPrefsDump(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(Config.PREFERENCES_BUCKET, MODE_PRIVATE);

        String dump = "\nterms agreed: " + renderBooleanPref(prefs, context, R.string.prefs_terms_agreed_key);
        dump += "\ntutorial completed: " + renderBooleanPref(prefs, context, R.string.prefs_tutorial_completed_key);
        dump += "\npurge on panic: " + renderBooleanPref(prefs, context, R.string.prefs_panic_purge_key);
        dump += "\nuninstall on panic: " + renderBooleanPref(prefs, context, R.string.prefs_panic_uninstall_key);
        dump += "\nhide BT stack broken: " + renderBooleanPref(prefs, context, R.string.prefs_hide_broken_bt_warning_key);
        dump += "\npeer retention: " + renderStringPref(prefs, context, R.string.prefs_retention_key);
        return dump;
    }
}
