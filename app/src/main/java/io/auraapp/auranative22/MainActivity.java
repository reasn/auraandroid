package io.auraapp.auranative22;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.auraapp.auranative22.Communicator.Communicator;
import io.auraapp.auranative22.Communicator.Peer;
import io.auraapp.auranative22.Communicator.Slogan;

import static io.auraapp.auranative22.Communicator.Communicator.INTENT_PEERS_CHANGED_ACTION;
import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";
    private BroadcastReceiver mMessageReceiver;

    private List<ListItem> mList = new ArrayList<>();
    private SloganListAdapter mListAdapter;
    private MySloganManager mMySloganManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        mMySloganManager = new MySloganManager(
                this,
                () -> {

                    int added = 0;
                    int removed = 0;

                    // Remove slogans that are gone
                    for (ListItem item : mList.subList(0, mList.size())) {
                        if (!mMySloganManager.getMySlogans().contains(item.getSlogan())) {
                            mList.remove(item);
                            removed++;
                        }
                    }

                    for (Slogan slogan : mMySloganManager.getMySlogans()) {
                        ListItem foundSlogan = new ListItem(slogan, true);
                        if (!mList.contains(foundSlogan)) {
                            mList.add(foundSlogan);
                            added++;
                        }
                    }

                    if (added > 0 || removed > 0) {
                        d(TAG, "My slogans changed, %d added, %d removed", added, removed);

                        mListAdapter.notifyDataSetChanged();
                        // TODO add argument or use added/removed to indicate whether dropped or adopted, update toast
                        Toast.makeText(getApplicationContext(), "Slogan changed ;) ", Toast.LENGTH_LONG).show();
                        advertiseSlogans();
                    }
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

        mListAdapter = new SloganListAdapter(this, mList);

        listView.setAdapter(mListAdapter);

        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {

            ListItem item = mList.get(position);
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

        checkPermissions();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {

        if (mMessageReceiver == null) {
            mMessageReceiver = new PeerSloganUpdateReceiver(mList, mListAdapter);
        }

        // TODO tell communicator to send all slogans over

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Communicator.INTENT_PEERS_CHANGED_ACTION));

        d(TAG, "Registered receiver for %s intents", INTENT_PEERS_CHANGED_ACTION);

        super.onResume();
    }

    private void advertiseSlogans() {

        List<Slogan> mySlogans = mMySloganManager.getMySlogans();

        v(TAG, "Advertising %d slogans", mySlogans.size());

        Intent intent = new Intent(this, Communicator.class);
        intent.setAction(Communicator.INTENT_LOCAL_SLOGANS_CHANGED_ACTION);
        if (mySlogans.size() > 0) {
            intent.putExtra(Communicator.INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_1, mySlogans.get(0).getText());
        }
        if (mySlogans.size() > 1) {
            intent.putExtra(Communicator.INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_2, mySlogans.get(1).getText());
        }
        if (mySlogans.size() > 2) {
            intent.putExtra(Communicator.INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_3, mySlogans.get(2).getText());
        }

        startService(intent);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);

                }
            } else {
//                Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();

                mMySloganManager.init();
            }
            // TODO handle user declining
        } else {
            mMySloganManager.init();
        }
    }
}
