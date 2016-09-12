package com.gnut3ll4.syncets.utils;

import android.content.Context;

import com.gnut3ll4.signetswebservices.soap.SignetsMobileSoap;
import com.gnut3ll4.syncets.ApplicationManager;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.model.GoogleEventWrapper;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.common.io.BaseEncoding;
import com.securepreferences.SecurePreferences;

import org.joda.time.DateTime;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rx.Observable;

public class GoogleCalendarUtils {

    private static SignetsMobileSoap signetsMobileSoap = new SignetsMobileSoap();

    public static String getETSCalendarId(Calendar calendarService, String calendarName) throws IOException {
        String calendarId = "";
        //If calendar exists on Google, we pick it.
        List<CalendarListEntry> items = calendarService.calendarList().list().execute().getItems();
        for (CalendarListEntry item : items) {
            if (item.getSummary().equals(calendarName)) {
                calendarId = item.getId();

            }
        }

        //Else, we create it.
        if (calendarId.isEmpty()) {
            com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
            calendar.setSummary(calendarName);
            calendarId = calendarService.calendars().insert(calendar).execute().getId();

        }
        return calendarId;
    }

    public static Observable<Event> getJoursRemplaces() {

        SimpleDateFormat joursRemplacesFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA_FRENCH);

        return Observable.just(signetsMobileSoap)
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
                        //TODO timezone offset
                        EventDateTime eventDateTime = new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(dateStart));

                        event.setStart(eventDateTime);
                        event.setEnd(eventDateTime.clone());

                        return Observable.just(event);
                    } catch (ParseException e) {
                        return Observable.error(e);
                    }
                });
    }

    //todo javadoc
    public static Observable<Event> getSeances(boolean notificationsActivated) {
        return Observable.just(signetsMobileSoap)
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

                    event.setSummary(seance.descriptionActivite.equals("Examen final") ?
                            "Examen final " + seance.coursGroupe :
                            seance.coursGroupe + " - " + seance.nomActivite);
                    event.setLocation(seance.local);

                    EventDateTime eventStartDateTime = new EventDateTime();
                    eventStartDateTime.setDateTime(new com.google.api.client.util.DateTime(seance.dateDebut.getTime() - Utils.getTimeZoneOffset()));
                    EventDateTime eventEndDateTime = new EventDateTime();
                    eventEndDateTime.setDateTime(new com.google.api.client.util.DateTime(seance.dateFin.getTime() - Utils.getTimeZoneOffset()));

                    event.setStart(eventStartDateTime);
                    event.setEnd(eventEndDateTime);

                    if (notificationsActivated) {
                        EventReminder[] reminderOverrides = new EventReminder[]{
                                new EventReminder().setMethod("popup").setMinutes(15),
                        };
                        Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(Arrays.asList(reminderOverrides));
                        event.setReminders(reminders);
                    }

                    return Observable.just(event);
                });
    }

    public static Observable<Object> syncGoogleCalendar(
            Observable<List<GoogleEventWrapper>> localEventsGoogle,
            Observable<List<GoogleEventWrapper>> remoteEventsSignets,
            Calendar calendarService,
            String calendarId) {

        return Observable.zip(localEventsGoogle, remoteEventsSignets,
                (localEvents, remoteEvents) -> {

                    try {
                        // Deletes entries in Google Calendar that don't exist on API
                        for (GoogleEventWrapper localObject : localEvents) {
                            if (!remoteEvents.contains(localObject)) {
                                calendarService.events().delete(calendarId, localObject.getEvent().getId()).execute();
                            }
                        }

                        // Adds new API entries on Google Calendar or updates existing ones
                        for (GoogleEventWrapper remoteObject : remoteEvents) {
                            try {
                                calendarService.events().get(calendarId, remoteObject.getEvent().getId()).execute();
                                calendarService.events().update(calendarId, remoteObject.getEvent().getId(), remoteObject.getEvent()).execute();
                            } catch (GoogleJsonResponseException e) {
                                if (e.getStatusCode() == 404) {
                                    calendarService.events().insert(calendarId, remoteObject.getEvent()).execute();
                                }
                            }
                        }

                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                    return Observable.empty();

                });
    }

    private static String calendarId;

    public static Observable<Object> syncCalendar(Context context, boolean notificationsActivated) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential = ApplicationManager.getGoogleCredentials(context);

        SecurePreferences securePreferences = new SecurePreferences(context);

        Calendar client = new Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("SyncETS")
                .build();

        //Checking if calendar exists and create it if not
        calendarId = securePreferences.getString(Constants.CALENDAR_ID, "");
        if (calendarId.isEmpty()) {
            try {
                calendarId = GoogleCalendarUtils.getETSCalendarId(
                        client,
                        context.getResources().getString(R.string.ets_calendar));
            } catch (IOException e) {
                return Observable.error(e);
            }
            securePreferences.edit().putString(Constants.CALENDAR_ID, calendarId).commit();
        }


        //Getting JoursRemplaces from Signets
        Observable<Event> eventJoursRemplacesObservable = GoogleCalendarUtils.getJoursRemplaces();

        //Getting Seances from Signets
        Observable<Event> eventSeancesObservable = GoogleCalendarUtils.getSeances(notificationsActivated);

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
        return GoogleCalendarUtils.syncGoogleCalendar(
                localEventsGoogle,
                remoteEventsSignets,
                client,
                calendarId);

    }
}

