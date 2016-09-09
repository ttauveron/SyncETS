package com.gnut3ll4.syncets.service;


import android.content.Intent;
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
    String tasklistId;

    public BackgroundService() {
        super("BackgroundService");
    }

    /**
     * Asynchronous background operations of service, with wakelock
     */
    @Override
    public void doWakefulWork(Intent intent) {
        Log.d("SYNCETS", "Started syncing");

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential;
        SecurePreferences securePreferences = new SecurePreferences(this);

        String selectedAccount = securePreferences.getString(Constants.SELECTED_ACCOUNT, "");

        credential = LoginActivity.mCredential;

        Calendar client = new Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("SyncETS")
                .build();


        Tasks taskClient = new Tasks.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("SyncETS")
                .build();


        //Checking if tasklist exists and create it if not
        if (!selectedAccount.isEmpty()) {
            try {
                tasklistId = GoogleTaskUtils.createETSTaskListId(taskClient,
                        getResources().getString(R.string.ets_tasklist));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //Sync Moodle Assignment in Google Task
        GoogleTaskUtils.getMoodleAssignmentsTaskEvents()
                .flatMap(task -> {
                    try {
                        taskClient.tasks().insert(tasklistId, task).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
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

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });


        //Checking if calendar exists and create it if not
        if (!selectedAccount.isEmpty()) {
            calendarId = securePreferences.getString(Constants.CALENDAR_ID, "");
            try {
                if (calendarId.isEmpty()) {
                    calendarId = GoogleCalendarUtils.getETSCalendarId(
                            client,
                            getResources().getString(R.string.ets_calendar));
                    securePreferences.edit().putString(Constants.CALENDAR_ID, calendarId).commit();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Getting JoursRemplaces from Signets
        Observable<Event> eventJoursRemplacesObservable = GoogleCalendarUtils.getJoursRemplaces();

        //Getting Seances from Signets
        Observable<Event> eventSeancesObservable = GoogleCalendarUtils.getSeances();

        //Merging list of calendar events (JoursRemplaces + Seances)
        Observable<List<GoogleEventWrapper>> remoteEventsSignets =
                Observable.merge(eventJoursRemplacesObservable, eventSeancesObservable)
                        .flatMap(event -> Observable.just(new GoogleEventWrapper(event)))
                        .toList();

        //Getting already created events in Google calendar
        Observable<List<GoogleEventWrapper>> localEventsGoogle = Observable.just(client.events())
                .flatMap(events1 -> {
                    try {
                        return Observable.from(events1.list(calendarId).execute().getItems());
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                })
                .flatMap(event1 -> Observable.just(new GoogleEventWrapper(event1)))
                .toList();


        //Syncing between Google calendar and Signets (updating Google calendar)
        GoogleCalendarUtils.syncGoogleCalendar(localEventsGoogle, remoteEventsSignets, client, calendarId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onCompleted() {
                        Log.d("SYNCETS", "Signets sync ended");
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