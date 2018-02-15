package io.auraapp.auraandroid.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.TreeSet;

import io.auraapp.auraandroid.Communicator.CommunicatorState;
import io.auraapp.auraandroid.PermissionMissingActivity;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.Peer;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.SloganComparator;
import io.auraapp.auraandroid.main.list.RecycleAdapter;

import static io.auraapp.auraandroid.common.EmojiHelper.replaceAppEmoji;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";

    static final String PREFS_BUCKET = "prefs";
    static final String PREFS_SLOGANS = "slogans";
    static final String PREFS_ENABLED = "enabled";

    final private TreeSet<Slogan> mPeerSlogans = new TreeSet<>(new SloganComparator());
    private RecycleAdapter mListAdapter;
    private MySloganManager mMySloganManager;
    private CommunicatorProxy mCommunicatorProxy;
    private boolean mAuraEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        setContentView(R.layout.activity_main);

        // Create toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(replaceAppEmoji(getString(R.string.ui_toolbar_title)));

        // Load preferences
        mAuraEnabled = getSharedPreferences(MainActivity.PREFS_BUCKET, MODE_PRIVATE).getBoolean(MainActivity.PREFS_ENABLED, true);

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
                (CommunicatorState state) -> {
                    // reflect health in UI below switch
                });

//        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        Button button = findViewById(R.id.add_slogan);
        button.setOnClickListener((View $$$) -> {

            View dialogView = MainActivity.this.getLayoutInflater().inflate(R.layout.dialog_add_slogan, null);

            TextView textView = dialogView.findViewById(R.id.dialog_add_slogan_slogan_text);
            new AlertDialog.Builder(MainActivity.this)
                    .setView(dialogView)
                    .setPositiveButton("Add", (DialogInterface $, int $$) -> {
                        mMySloganManager.adopt(Slogan.create(textView.getText().toString()));
                    })
                    .setNegativeButton("Cancel", (DialogInterface $, int $$) -> {
                    })
                    .create()
                    .show();
        });
        createListView();

        mMySloganManager.init();
        mListAdapter.notifySlogansChanged();
        mCommunicatorProxy.updateMySlogans(mMySloganManager.getMySlogans());
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
                            .setPositiveButton("Delete", (DialogInterface $, int $$) -> {
                                mMySloganManager.dropSlogan(slogan);
                            })
                            .setNegativeButton("Cancel", (DialogInterface $, int $$) -> {
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
                ? R.string.ui_enable_aura_on
                : R.string.ui_enable_aura_off));

        enabledSwitch.setOnCheckedChangeListener((CompoundButton $, boolean isChecked) -> {
            mAuraEnabled = isChecked;
            getSharedPreferences(MainActivity.PREFS_BUCKET, MODE_PRIVATE).edit()
                    .putBoolean(MainActivity.PREFS_ENABLED, isChecked)
                    .apply();
            if (isChecked) {
                mCommunicatorProxy.enable();
                enabledSwitch.setText(getString(R.string.ui_enable_aura_on));
            } else {
                enabledSwitch.setText(getString(R.string.ui_enable_aura_off));
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
    }

    @Override
    protected void onPause() {
        mCommunicatorProxy.stopListening();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
