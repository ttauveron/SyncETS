package com.gnut3ll4.syncets.service;


import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import com.gnut3ll4.signetswebservices.soap.SignetsMobileSoap;
import com.gnut3ll4.syncets.ApplicationManager;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.model.GoogleEventWrapper;
import com.gnut3ll4.syncets.model.SeancesWrapper;
import com.gnut3ll4.syncets.ui.LoginActivity;
import com.gnut3ll4.syncets.utils.Constants;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarRequestInitializer;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.common.io.BaseEncoding;
import com.securepreferences.SecurePreferences;

import org.joda.time.DateTime;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
                    //If calendar exists on Google, we pick it.
                    List<CalendarListEntry> items = client.calendarList().list().execute().getItems();
                    for (CalendarListEntry item : items) {
                        if (item.getSummary().equals(getResources().getString(R.string.ets_calendar))) {
                            calendarId = item.getId();
                            securePreferences.edit().putString(Constants.CALENDAR_ID, calendarId).commit();
                        }
                    }

                    //Else, we create it.
                    if (calendarId.isEmpty()) {
                        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
                        calendar.setSummary(getResources().getString(R.string.ets_calendar));
                        calendarId = client.calendars().insert(calendar).execute().getId();
                        securePreferences.edit().putString(Constants.CALENDAR_ID, calendarId).commit();

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Inserting JoursRemplaces in local calendar
        SimpleDateFormat joursRemplacesFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA_FRENCH);
        //Inserting Seances in local calendar
        SimpleDateFormat seancesFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.CANADA_FRENCH);


        ArrayList<Event> events = new ArrayList<>();

        //Getting and syncing in DB JoursRemplaces from Signets
        Observable<Event> eventJoursRemplacesObservable =
                Observable.just(signetsMobileSoap)
                        .flatMap(signetsMobileSoap1 -> {
                            try {
                                return Observable.just(
                                        signetsMobileSoap1.listeSessions(
                                                ApplicationManager.userCredentials.getUsername(),
                                                ApplicationManager.userCredentials.getPassword()));
                            } catch (Exception e) {
                                return Observable.error(e);
                            }
                        })
                        .flatMap(listeDeSessions -> Observable.from(listeDeSessions.liste))
                        .filter(trimestre -> {
                            DateTime dtStart = new DateTime();
                            DateTime dtEnd = new DateTime(trimestre.dateFin);

                            return dtStart.isBefore(dtEnd.plusDays(1));
                        })
                        .flatMap(trimestre1 -> {

                            try {
                                return Observable.just(signetsMobileSoap.lireJoursRemplaces(trimestre1.abrege).listeJours);
                            } catch (Exception e) {
                                return Observable.error(e);
                            }

                        })
                        .flatMap(Observable::from)
                        .flatMap(joursRemplaces -> {
                            try {
                                Event event = new Event();
                                String encodedId = BaseEncoding.base32Hex()
                                        .encode(joursRemplaces.dateOrigine.getBytes())
                                        .toLowerCase()
                                        .replace("=", "");

                                event.setId(encodedId);
                                event.setSummary(joursRemplaces.description);

                                Date dateStart = joursRemplacesFormatter.parse(joursRemplaces.dateOrigine);
                                EventDateTime eventDateTime = new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(dateStart));

                                event.setStart(eventDateTime);
                                event.setEnd(eventDateTime.clone());

                                return Observable.just(event);
                            } catch (ParseException e) {
                                return Observable.error(e);
                            }
                        });

        //Getting and syncing in DB Seances from Signets
        Observable<Event> eventSeancesObservable =
                Observable.just(signetsMobileSoap)
                        .flatMap(signetsMobileSoap1 -> {
                            try {
                                return Observable.just(
                                        signetsMobileSoap1.listeSessions(
                                                ApplicationManager.userCredentials.getUsername(),
                                                ApplicationManager.userCredentials.getPassword()));
                            } catch (Exception e) {
                                return Observable.error(e);
                            }
                        })
                        .flatMap(listeDeSessions -> Observable.from(listeDeSessions.liste))
                        .filter(trimestre -> {
                            DateTime dtStart = new DateTime();
                            DateTime dtEnd = new DateTime(trimestre.dateFin);

                            return dtStart.isBefore(dtEnd.plusDays(1));
                        })
                        .flatMap(trimestre1 -> {
                            try {
                                return Observable.just(signetsMobileSoap.lireHoraireDesSeances(
                                        ApplicationManager.userCredentials.getUsername(),
                                        ApplicationManager.userCredentials.getPassword(), "",
                                        trimestre1.abrege, "", "")
                                        .listeDesSeances);
                            } catch (Exception e) {
                                return Observable.error(e);
                            }

                        })
                        .flatMap(Observable::from)
                        .flatMap(seance -> {
                            Event event = new Event();
                            String encodedId = BaseEncoding.base32Hex()
                                    .encode(seance.getId().getBytes())
                                    .toLowerCase()
                                    .replace("=", "");
                            event.setId(encodedId);
                            event.setSummary(seance.descriptionActivite.equals("Examen final") ? "Examen final " + seance.coursGroupe : seance.coursGroupe);
                            event.setLocation(seance.local);

                            int GMT4 = 3600*1000*4;
                            EventDateTime eventStartDateTime = new EventDateTime();
                            eventStartDateTime.setDateTime(new com.google.api.client.util.DateTime(seance.dateDebut.getTime()+GMT4));
                            EventDateTime eventEndDateTime = new EventDateTime();
                            eventEndDateTime.setDateTime(new com.google.api.client.util.DateTime(seance.dateFin.getTime()+GMT4));

                            event.setStart(eventStartDateTime);
                            event.setEnd(eventEndDateTime);

                            SharedPreferences defaultSharedPreferences = PreferenceManager
                                    .getDefaultSharedPreferences(this);
                            //TODO enable notifications
//                                boolean activateNotifications = defaultSharedPreferences
//                                        .getBoolean(getString(R.string.preference_key), true);
//                                if (activateNotifications) {
//                                    EventReminder[] reminderOverrides = new EventReminder[] {
//                                            new EventReminder().setMethod("popup").setMinutes(30),
//                                    };
//                                    Event.Reminders reminders = new Event.Reminders()
//                                            .setUseDefault(false)
//                                            .setOverrides(Arrays.asList(reminderOverrides));
//                                    event.setReminders(reminders);
//                                }

                            return Observable.just(event);
                        });


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
        Observable.zip(localEventsGoogle, remoteEventsSignets,
                (localEvents, remoteEvents) -> {

                    try {
                        // Deletes entries in Google Calendar that don't exist on API
                        for (GoogleEventWrapper localObject : localEvents) {
                            if (!remoteEvents.contains(localObject)) {
                                client.events().delete(calendarId, localObject.getEvent().getId()).execute();
                            }
                        }

                        // Adds new API entries on Google Calendar or updates existing ones
                        for (GoogleEventWrapper remoteObject : remoteEvents) {
                            try {
                                client.events().get(calendarId, remoteObject.getEvent().getId()).execute();
                                client.events().update(calendarId, remoteObject.getEvent().getId(), remoteObject.getEvent()).execute();
                            } catch (GoogleJsonResponseException e) {
                                if (e.getStatusCode() == 404) {
                                    client.events().insert(calendarId, remoteObject.getEvent()).execute();
                                }
                            }
                        }

                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                    return null;

                })
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