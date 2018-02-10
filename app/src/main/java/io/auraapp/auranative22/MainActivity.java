package io.auraapp.auranative22;

import android.Manifest;
import android.content.BroadcastReceiver;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
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

    //    private static final String slogan1 = Build.VERSION.SDK_INT + " " + android.os.Build.MODEL + " Helloo World, Hello!1Helloo World, 🚀 Hello!1Helloo World, Hello!1Helloo World, H";
//    private static final String slogan2 = Build.VERSION.SDK_INT + " " + android.os.Build.MODEL + " Fappoo LorpdFappo!1FappoLorpdFappo!1FappoLorpdFappo!1FappoLorpdF nanunana wadatap";
//    private static final String slogan3 = Build.VERSION.SDK_INT + " " + android.os.Build.MODEL + " The lazy chicken jumps over the running dog on its way to the alphabet. Cthullu !";
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

        if (mSlogans.size() == 0) {
            mSlogans.add(Slogan.create(true, Build.VERSION.SDK_INT + " " + android.os.Build.MODEL + " The lazy chicken jumps over the running dog on its way to the alphabet. Cthullu !"));
            persistSlogans();
        }

        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.list_view);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        listView.setEmptyView(progressBar);

        // Must add the progress bar to the root of the layout
        ViewGroup root = findViewById(android.R.id.content);
        root.addView(progressBar);

        mListAdapter = new SloganListAdapter(this, mSlogans);

        listView.setAdapter(mListAdapter);

        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Toast.makeText(getApplicationContext(), "Click " + mSlogans.get(position).mText, Toast.LENGTH_SHORT).show();
        });

        checkPermissions();
    }

    private void persistSlogans() {

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_BUCKET, MODE_PRIVATE).edit();
        Set<String> mySloganTexts = new HashSet<>();
        for (Slogan slogan : mSlogans) {
            mySloganTexts.add(slogan.mText);
        }
        editor.putStringSet(PREFS_SLOGANS, mySloganTexts);

        editor.apply();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {

        if (mMessageReceiver == null) {
            mMessageReceiver = new PeerSloganUpdateReceiver(mSlogans, mListAdapter);
        }

        // TODO tell communicator to send all slogans over

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
