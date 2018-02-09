package io.auraapp.auranative22;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import io.auraapp.auranative22.Communicator.Communicator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startCommunicator();
        ListView listView = findViewById(R.id.list_view);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        listView.setEmptyView(progressBar);

        // Must add the progress bar to the root of the layout
        ViewGroup root = findViewById(android.R.id.content);
        root.addView(progressBar);

        final ArrayList<Slogan> list = new ArrayList<>();
        list.add(Slogan.create(true, "Hallo"));
        list.add(Slogan.create(true, "Fisch"));
        list.add(Slogan.create(true, "Android"));
        list.add(Slogan.create(true, "Ubuntu"));
        list.add(Slogan.create(true, "Windows7"));
        list.add(Slogan.create(true, "WebOS"));

        ArrayAdapter<Slogan> adapter = new ArrayAdapter<Slogan>(this, R.layout.list_item, list) {
            @Override
            public void notifyDataSetChanged() {
                Collections.sort(list, (Slogan o1, Slogan o2) -> {
                    if (o1.mMine && !o2.mMine) {
                        return -1;
                    }
                    if (!o1.mMine && o2.mMine) {
                        return 1;
                    }
                    return o1.mText.compareTo(o2.mText);
                });
                super.notifyDataSetChanged();
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                // Get the data item for this position
                Slogan user = getItem(position);
                // Check if an existing view is being reused, otherwise inflate the view
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
                }
                TextView tvName = convertView.findViewById(R.id.slogan_text);
                if(user != null) {
                    tvName.setText(user.mText);
                } else {
                    tvName.setText("");
                }
                return convertView;
            }
        };

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Toast.makeText(getApplicationContext(), "Click " + list.get(position).mText, Toast.LENGTH_SHORT).show();
        });

    }

    private void startCommunicator() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);

                }
            } else {
                Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
                startService(new Intent(this, Communicator.class));
            }
            // TODO handle user declining
        } else {
            startService(new Intent(this, Communicator.class));
        }
    }
}
