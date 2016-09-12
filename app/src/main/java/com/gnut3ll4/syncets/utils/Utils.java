package com.gnut3ll4.syncets.utils;

import com.securepreferences.SecurePreferences;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Utils {

    /**
     * Returns the current code session : {Year}{1/2/3}
     * @return
     */
    public static String getCurrentCodeSession() {
        DateTime now = DateTime.now();
        int codeSession;
        switch (now.getMonthOfYear()) {
            case 1:
            case 2:
            case 3:
            case 4:
                codeSession = 1;
                break;
            case 5:
            case 6:
            case 7:
            case 8:
                codeSession = 2;
                break;
            case 9:
            case 10:
            case 11:
            case 12:
                codeSession = 3;
                break;
            default:
                codeSession = 0;
                break;
        }
        return now.getYear()+""+codeSession;
    }

    public static long getTimeZoneOffset() {

        //todo fix timezone
        TimeZone mTimeZone = new GregorianCalendar().getTimeZone();
        return mTimeZone.getRawOffset();
    }

    public static Date getDate(final SecurePreferences prefs, final String key, final Date defValue) {
        if (!prefs.contains(key + "_value") || !prefs.contains(key + "_zone")) {
            return defValue;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(prefs.getLong(key + "_value", 0));
        calendar.setTimeZone(TimeZone.getTimeZone(prefs.getString(key + "_zone", TimeZone.getDefault().getID())));
        return calendar.getTime();
    }

    public static void putDate(final SecurePreferences prefs, final String key, final Date date, final TimeZone zone) {
        prefs.edit().putLong(key + "_value", date.getTime()).apply();
        prefs.edit().putString(key + "_zone", zone.getID()).apply();
    }
}
