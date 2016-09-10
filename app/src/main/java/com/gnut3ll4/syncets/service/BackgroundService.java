package com.gnut3ll4.syncets.service;


import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.model.GoogleEventWrapper;
import com.gnut3ll4.syncets.ui.LoginActivity;
import com.gnut3ll4.syncets.utils.Constants;
import com.gnut3ll4.syncets.utils.GoogleCalendarUtils;
import com.gnut3ll4.syncets.utils.GoogleTaskUtils;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.tasks.Tasks;
import com.securepreferences.SecurePreferences;

import java.io.IOException;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by gnut3ll4 on 26/03/16.
 */
public class BackgroundService extends WakefulIntentService {

    String calendarId;


    public BackgroundService() {
        super("BackgroundService");
    }

    /**
     * Asynchronous background operations of service, with wakelock
     */
    @Override
    public void doWakefulWork(Intent intent) {
        Log.d("SYNCETS", "Started syncing");


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean pref_sync_courses = prefs.getBoolean("pref_sync_courses", true);
        boolean pref_sync_moodle = prefs.getBoolean("pref_sync_moodle", true);
        boolean pref_notif_courses = prefs.getBoolean("pref_notif_courses", true);

        try {
            if(pref_sync_courses)
                GoogleCalendarUtils.syncCalendar(this, pref_notif_courses);
            if(pref_sync_moodle)
                GoogleTaskUtils.syncMoodleAssignments(this);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}