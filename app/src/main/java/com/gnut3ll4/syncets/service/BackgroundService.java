package com.gnut3ll4.syncets.service;


import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import com.gnut3ll4.signetswebservices.soap.SignetsMobileSoap;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.model.GoogleEventWrapper;
import com.gnut3ll4.syncets.ui.LoginActivity;
import com.gnut3ll4.syncets.utils.Constants;
import com.gnut3ll4.syncets.utils.GoogleCalendarUtils;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.securepreferences.SecurePreferences;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        Log.e("TEST", "BACKGROUND TASK EXECUTING.......");


        Calendar client;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential;
        SecurePreferences securePreferences = new SecurePreferences(this);

        String selectedAccount = securePreferences.getString(Constants.SELECTED_ACCOUNT, "");

        credential = LoginActivity.mCredential;

        client = new Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("SyncETS")
                .build();

        SignetsMobileSoap signetsMobileSoap = new SignetsMobileSoap();

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

        //Inserting JoursRemplaces in local calendar

        //Inserting Seances in local calendar
        SimpleDateFormat seancesFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.CANADA_FRENCH);


        ArrayList<Event> events = new ArrayList<>();

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
                        Log.d("SYNC", "SYNC COMPLETED");
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