package com.gnut3ll4.syncets.ui;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.utils.GoogleCalendarUtils;
import com.gnut3ll4.syncets.utils.GoogleTaskUtils;

import java.io.IOException;

public class PreferenceSyncFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
//        setContentView(R.layout.fragment_main);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, null);

        Button test = (Button) view.findViewById(R.id.btn_test);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {
                    protected void onPreExecute() {
                        Log.d("SYNC", "Sync started");
                    }
                    protected Void doInBackground(Void... unused) {
                        try {
                            GoogleCalendarUtils.syncCalendar(getActivity(), true);
                            GoogleTaskUtils.syncMoodleAssignments(getActivity());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                    protected void onPostExecute(Void unused) {
                        Log.d("SYNC", "Sync ended");
                    }
                }.execute();
            }
        });

        return view;

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // handle the preference change here
        switch (key) {
            case "pref_sync_courses":
                if (sharedPreferences.getBoolean(key, true)) {

                }
                break;
            case "pref_sync_moodle":
                break;
            case "pref_notif_courses":
                break;
        }


    }
}