package com.gnut3ll4.syncets.service;


import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

/**
 * Created by gnut3ll4 on 26/03/16.
 */
public class BackgroundService extends WakefulIntentService {


    public BackgroundService() {
        super("BackgroundService");
    }

    /**
     * Asynchronous background operations of service, with wakelock
     */
    @Override
    public void doWakefulWork(Intent intent) {
        Log.e("TEST", "BACKGROUND TASK EXECUTING.......");
    }
}