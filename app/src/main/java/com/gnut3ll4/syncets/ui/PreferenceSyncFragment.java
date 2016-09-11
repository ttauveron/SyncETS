package com.gnut3ll4.syncets.ui;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.utils.GoogleCalendarUtils;
import com.gnut3ll4.syncets.utils.GoogleTaskUtils;

import java.io.IOException;

import mehdi.sakout.fancybuttons.FancyButton;

public class PreferenceSyncFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
//        setContentView(R.layout.fragment_main);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        onCoachMark();
    }

    public void onCoachMark(){

        final Dialog dialog = new Dialog(getActivity(), R.style.WalkthroughTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.coach_mark);
        dialog.setCanceledOnTouchOutside(true);
        //for dismissing anywhere you touch
        View masterView = dialog.findViewById(R.id.coach_mark_master_view);
        View button = dialog.findViewById(R.id.btn_sync_overlay);
        button.setEnabled(false);
        View.OnClickListener dismissOnClick = view -> dialog.dismiss();
        masterView.setOnClickListener(dismissOnClick);
        button.setOnClickListener(dismissOnClick);
        dialog.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, null);


        Preference pref = findPreference("pref_static_field_key");
        String summary = (String) pref.getSummary();

        //Update the summary with user input data
        pref.setSummary("10/10/2010 14h34");

        FancyButton syncButton = (FancyButton) view.findViewById(R.id.btn_sync);

        syncButton.setOnClickListener(new View.OnClickListener() {
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