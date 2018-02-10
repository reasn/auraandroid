package io.auraapp.auranative22;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

import static io.auraapp.auranative22.Communicator.Communicator.INTENT_PEERS_CHANGED_ACTION;
import static io.auraapp.auranative22.FormattedLog.d;
import static io.auraapp.auranative22.FormattedLog.v;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "@aura/main";
    private BroadcastReceiver mMessageReceiver;

    private static final String PREFS_BUCKET = "prefs";
    private static final String PREFS_SLOGANS = "slogans";

    private List<Slogan> mSlogans = new ArrayList<>();
    private ArrayAdapter<Slogan> mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v(TAG, "onCreate, intent: %s", getIntent().getAction());

        mSlogans = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences(PREFS_BUCKET, MODE_PRIVATE);
        for (String mySloganText : prefs.getStringSet(PREFS_SLOGANS, new HashSet<>())) {
            mSlogans.add(Slogan.create(true, mySloganText));
        }

        mSlogans.add(Slogan.create(false, "adopt me :)"));

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
                        adoptSlogan(textView.getText().toString());
                    })
                    .setNegativeButton("Cancel", (DialogInterface $, int $$) -> {
                    })
                    .create()
                    .show();
        });

        mListAdapter = new SloganListAdapter(this, mSlogans);

        listView.setAdapter(mListAdapter);

        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {

            Slogan slogan = mSlogans.get(position);
            if (!slogan.mMine) {

                if (countMySlogans() >= 3) {
                    // Replace slogan
                    // TODO implement
                } else {
                    adoptSlogan(slogan.mText);
                }
            } else {
                new AlertDialog.Builder(activity)
                        .setPositiveButton("Delete", (DialogInterface $, int $$) -> {
                            dropSlogan(slogan.mText);
                        })
                        .setNegativeButton("Cancel", (DialogInterface $, int $$) -> {
                        })
                        .create()
                        .show();
            }
        });

        checkPermissions();
    }

    private int countMySlogans() {
        int mine = 0;
        for (Slogan s : mSlogans) {
            if (s.mMine) {
                mine++;
            }
        }
        return mine;
    }

    private void dropSlogan(String text) {
        mSlogans.remove(Slogan.create(true, text));

        persistSlogans();
        mListAdapter.notifyDataSetChanged();
        Toast.makeText(getApplicationContext(), "Slogan dropped ", Toast.LENGTH_LONG).show();
        advertiseSlogans();
    }

    private void adoptSlogan(String text) {
        if (countMySlogans() >= 3) {
            return;
        }
        mSlogans.remove(Slogan.create(true, text));
        mSlogans.remove(Slogan.create(false, text));
        mSlogans.add(Slogan.create(true, text));

        persistSlogans();
        mListAdapter.notifyDataSetChanged();
        Toast.makeText(getApplicationContext(), "Slogan adopted ", Toast.LENGTH_LONG).show();
        advertiseSlogans();
    }

    private void persistSlogans() {

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_BUCKET, MODE_PRIVATE).edit();
        Set<String> mySloganTexts = new HashSet<>();
        for (Slogan slogan : mSlogans) {
            if (slogan.mMine) {
                mySloganTexts.add(slogan.mText);
            }
        }
        editor.putStringSet(PREFS_SLOGANS, mySloganTexts);

        editor.apply();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {

        if (mMessageReceiver == null) {
            mMessageReceiver = new PeerSloganUpdateReceiver(mSlogans, mListAdapter);
        }

        // TODO tell communicator to send all slogans over
        // TODO send all present slogans at once

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Communicator.INTENT_PEERS_CHANGED_ACTION));

        d(TAG, "Registered receiver for %s intents", INTENT_PEERS_CHANGED_ACTION);

        super.onResume();
    }

    private void advertiseSlogans() {

        Intent intent = new Intent(this, Communicator.class);
        intent.setAction(Communicator.INTENT_LOCAL_SLOGANS_CHANGED_ACTION);
        if (mSlogans.size() > 0) {
            intent.putExtra(Communicator.INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_1, mSlogans.get(0).mText);
        }
        if (mSlogans.size() > 1) {
            intent.putExtra(Communicator.INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_2, mSlogans.get(1).mText);
        }
        if (mSlogans.size() > 2) {
            intent.putExtra(Communicator.INTENT_LOCAL_SLOGANS_CHANGED_SLOGAN_3, mSlogans.get(2).mText);
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
                advertiseSlogans();
            }
            // TODO handle user declining
        } else {
            advertiseSlogans();
        }
    }
}
