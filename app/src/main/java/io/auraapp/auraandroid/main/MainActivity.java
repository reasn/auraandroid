package io.auraapp.auraandroid.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.TreeSet;

import io.auraapp.auraandroid.Communicator.Communicator;
import io.auraapp.auraandroid.PeerSloganUpdateReceiver;
import io.auraapp.auraandroid.PermissionMissingActivity;
import io.auraapp.auraandroid.R;
import io.auraapp.auraandroid.common.PermissionHelper;
import io.auraapp.auraandroid.common.Slogan;
import io.auraapp.auraandroid.common.SloganComparator;

import static io.auraapp.auraandroid.Communicator.Communicator.INTENT_PEERS_CHANGED_ACTION;
import static io.auraapp.auraandroid.common.FormattedLog.d;
import static io.auraapp.auraandroid.common.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";
    private BroadcastReceiver mMessageReceiver;

    final private TreeSet<Slogan> mPeerSlogans = new TreeSet<>(new SloganComparator());
    private SloganListAdapter mListAdapter;
    private MySloganManager mMySloganManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        mMySloganManager = new MySloganManager(
                this,
                () -> {
                    d(TAG, "My slogans changed");

                    mListAdapter.notifyDataSetChanged();
                    // TODO add argument or use added/removed to indicate whether dropped or adopted, update toast
                    Toast.makeText(getApplicationContext(), "Slogan changed ;) ", Toast.LENGTH_LONG).show();
                    advertiseSlogans();
                }
        );

        setContentView(R.layout.activity_main);

        final Activity activity = this;

        // TODO make list expandable to show stats for slogans
        ListView listView = findViewById(R.id.list_view);
        Button button = findViewById(R.id.add_slogan);
        button.setOnClickListener((View $$$) -> {

            View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_add_slogan, null);

            TextView textView = dialogView.findViewById(R.id.dialog_add_slogan_slogan_text);
            new AlertDialog.Builder(activity)
                    .setView(dialogView)
                    .setPositiveButton("Add", (DialogInterface $, int $$) -> {
                        mMySloganManager.adopt(Slogan.create(textView.getText().toString()));
                    })
                    .setNegativeButton("Cancel", (DialogInterface $, int $$) -> {
                    })
                    .create()
                    .show();
        });

        mListAdapter = SloganListAdapter.create(this, mMySloganManager.getMySlogans(), mPeerSlogans);

        listView.setAdapter(mListAdapter);

        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {

            ListItem item = mListAdapter.getItem(position);
            if (item == null) {
                return;
            }
            if (!item.isMine()) {

                if (mMySloganManager.spaceAvailable()) {
                    mMySloganManager.adopt(item.getSlogan());
                } else {
                    // Replace slogan
                    // TODO implement
                }
            } else {
                new AlertDialog.Builder(activity)
                        .setPositiveButton("Delete", (DialogInterface $, int $$) -> {
                            mMySloganManager.dropSlogan(item.getSlogan());
                        })
                        .setNegativeButton("Cancel", (DialogInterface $, int $$) -> {
                        })
                        .create()
                        .show();
            }
        });

        mMySloganManager.init();
    }

    @Override
    protected void onPause() {
        if (mMessageReceiver != null) {
            unregisterReceiver(mMessageReceiver);
        }
        super.onPause();
    }

    private void showPermissionMissingActivity() {

        Intent intent = new Intent(this, PermissionMissingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!PermissionHelper.granted(this)) {
            showPermissionMissingActivity();
            return;
        }

        if (mMessageReceiver == null) {
            mMessageReceiver = new PeerSloganUpdateReceiver(mPeerSlogans, mListAdapter);
        }

        // TODO tell communicator to send all slogans over

        registerReceiver(mMessageReceiver, new IntentFilter(Communicator.INTENT_PEERS_CHANGED_ACTION));
        d(TAG, "Registered receiver for %s intents", INTENT_PEERS_CHANGED_ACTION);
    }

    private void advertiseSlogans() {
        TreeSet<Slogan> slogans = mMySloganManager.getMySlogans();

        v(TAG, "Advertising %d slogans", slogans.size());

        Intent intent = new Intent(this, Communicator.class);
        intent.setAction(Communicator.INTENT_MY_SLOGANS_CHANGED_ACTION);

        String[] mySloganStrings = new String[slogans.size()];
        int index = 0;
        for (Slogan slogan : slogans) {
            mySloganStrings[index++] = slogan.getText();
        }
        intent.putExtra(Communicator.INTENT_MY_SLOGANS_CHANGED_SLOGANS, mySloganStrings);
        startService(intent);
    }
}
