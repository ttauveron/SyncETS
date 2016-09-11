package com.gnut3ll4.syncets.utils;

import org.joda.time.DateTime;

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
}
