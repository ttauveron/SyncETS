package com.gnut3ll4.syncets;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.gnut3ll4.syncets.model.UserCredentials;
import com.gnut3ll4.syncets.ui.SettingsActivity;
import com.securepreferences.SecurePreferences;

public class ApplicationManager extends Application {

    public static UserCredentials userCredentials;

    @Override
    public void onCreate() {
        super.onCreate();

        SecurePreferences securePreferences = new SecurePreferences(this);
        String u = securePreferences.getString(UserCredentials.CODE_U, "");
        String p = securePreferences.getString(UserCredentials.CODE_P, "");

        if (!TextUtils.isEmpty(u) && !TextUtils.isEmpty(p)) {
            userCredentials = new UserCredentials(u, p);
        }
    }

    public static void logout(final Activity activity) {

        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        editor.clear();
        editor.commit();

        WakefulIntentService.cancelAlarms(activity);

        ApplicationManager.userCredentials = null;
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        new Thread(new Runnable() {

            @Override
            public void run() {
                activity.finish();
            }
        }).start();

    }
}
