package com.gnut3ll4.syncets.service;


import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.gnut3ll4.syncets.utils.GoogleCalendarUtils;
import com.gnut3ll4.syncets.utils.GoogleTaskUtils;

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

//todo 1 sync by day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean pref_sync_courses = prefs.getBoolean("pref_sync_courses", true);
        boolean pref_sync_moodle = prefs.getBoolean("pref_sync_moodle", true);
        boolean pref_notif_courses = prefs.getBoolean("pref_notif_courses", true);

        if (pref_sync_courses)
            GoogleCalendarUtils.syncCalendar(this, pref_notif_courses)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Object>() {
                        @Override
                        public void onCompleted() {
                            Log.d("SYNCETS", "Calendar sync ended");
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(Object o) {

                        }
                    });

        if (pref_sync_moodle)
            GoogleTaskUtils.syncMoodleAssignments(this)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Object>() {
                        @Override
                        public void onCompleted() {
                            Log.d("SYNCETS", "Moodle sync ended");
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(Object o) {

                        }
                    });


    }
}