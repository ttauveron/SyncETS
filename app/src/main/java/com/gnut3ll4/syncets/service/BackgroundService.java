package com.gnut3ll4.syncets.service;


import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.gnut3ll4.syncets.utils.Constants;
import com.gnut3ll4.syncets.utils.GoogleCalendarUtils;
import com.gnut3ll4.syncets.utils.GoogleTaskUtils;
import com.gnut3ll4.syncets.utils.Utils;
import com.securepreferences.SecurePreferences;

import java.util.Date;
import java.util.GregorianCalendar;

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
        SecurePreferences securePreferences = new SecurePreferences(this);

        boolean syncAvailableToday = Utils.isSyncAvailableToday(this);
        if(!syncAvailableToday) {
            return;
        }

        boolean pref_sync_courses = prefs.getBoolean("pref_sync_courses", true);
        boolean pref_sync_moodle = prefs.getBoolean("pref_sync_moodle", true);
        boolean pref_notif_courses = prefs.getBoolean("pref_notif_courses", true);

        Observable<Object> syncCalendar = Observable.empty();
        Observable<Object> syncMoodleAssignments = Observable.empty();

        if (pref_sync_courses) {
            syncCalendar = GoogleCalendarUtils.syncCalendar(this, pref_notif_courses);
        }
        if (pref_sync_moodle) {
            syncMoodleAssignments = GoogleTaskUtils.syncMoodleAssignments(this);
        }

        rx.Observable.merge(syncCalendar, syncMoodleAssignments)
                .flatMap(o -> {
                    Utils.putDate(securePreferences,
                            Constants.LAST_SYNC,
                            new Date(),
                            new GregorianCalendar().getTimeZone());
                    return Observable.empty();
                })
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