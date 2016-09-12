package com.gnut3ll4.syncets.ui;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.utils.Constants;
import com.gnut3ll4.syncets.utils.GoogleCalendarUtils;
import com.gnut3ll4.syncets.utils.GoogleTaskUtils;
import com.gnut3ll4.syncets.utils.Utils;
import com.securepreferences.SecurePreferences;

import java.util.Date;
import java.util.GregorianCalendar;

import mehdi.sakout.fancybuttons.FancyButton;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PreferenceSyncFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    SecurePreferences securePreferences;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
        securePreferences = new SecurePreferences(getActivity());

        boolean firstLogin = securePreferences.getBoolean(Constants.FIRST_LOGIN, true);
        if (firstLogin) {
            securePreferences.edit().putBoolean(Constants.FIRST_LOGIN, false).commit();
            onCoachMark();
        }
    }

    public void onCoachMark() {
        final Dialog dialog = new Dialog(getActivity(), R.style.WalkthroughTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.coach_mark);
        dialog.setCanceledOnTouchOutside(true);
        //for dismissing anywhere you touch
        View masterView = dialog.findViewById(R.id.coach_mark_master_view);
        View button = dialog.findViewById(R.id.btn_sync_overlay);
        View buttonOk = dialog.findViewById(R.id.btn_ok);
        button.setEnabled(false);
        View.OnClickListener dismissOnClick = view -> dialog.dismiss();
        masterView.setOnClickListener(dismissOnClick);
        button.setOnClickListener(dismissOnClick);
        buttonOk.setOnClickListener(dismissOnClick);
        dialog.show();
        //todo E/WindowManager: android.view.WindowLeaked when logout
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, null);

        //todo last sync display
        updateLastSync();


        FancyButton syncButton = (FancyButton) view.findViewById(R.id.btn_sync);
        CircularProgressView circularProgressView = (CircularProgressView) view.findViewById(R.id.progress_view);

        syncButton.setOnClickListener(view1 -> {

                    //todo display message like "this can take several minutes"
                    circularProgressView.setVisibility(View.VISIBLE);
                    syncButton.setVisibility(View.INVISIBLE);

                    //TODO add preference check
                    rx.Observable.create(subscriber -> {


                        getSyncObservable().subscribe(new Observer<Object>() {
                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(Object o) {

                            }
                        });
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<Object>() {
                                @Override
                                public void onCompleted() {
                                    Log.d("SYNCETS", "UI sync ended");
                                    //todo add animation translate here
                                    circularProgressView.setVisibility(View.GONE);
                                    syncButton.setVisibility(View.GONE);
                                    updateLastSync();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    e.printStackTrace();
                                    circularProgressView.setVisibility(View.GONE);
                                    syncButton.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onNext(Object o) {

                                }
                            });
                }

        );


        return view;

    }

    public void updateLastSync() {
        Preference pref = findPreference("pref_static_field_key");
        Date date = Utils.getDate(securePreferences, Constants.LAST_SYNC, null);
        pref.setSummary(date == null ? "Never" : date.toString());
    }

    public rx.Observable<Object> getSyncObservable() {

        //todo choose boolean sync options
        rx.Observable<Object> syncCalendarEnded = GoogleCalendarUtils.syncCalendar(getActivity(), true)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        rx.Observable<Object> syncMoodleEnded = GoogleTaskUtils.syncMoodleAssignments(getActivity())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        return Observable.merge(syncCalendarEnded, syncMoodleEnded)
                .flatMap(o -> {
                    Utils.putDate(securePreferences,
                            Constants.LAST_SYNC,
                            new Date(),
                            new GregorianCalendar().getTimeZone());
                    return Observable.empty();
                });

//        Observable<Object> zip = Observable.zip(syncCalendarEnded, syncMoodleEnded, (o, o2) -> {
//            Utils.putDate(securePreferences,
//                    Constants.LAST_SYNC,
//                    new Date(),
//                    new GregorianCalendar().getTimeZone());
//            return Observable.empty();
//        });
//
//        return zip;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // handle the preference change here
        switch (key) {
            case "pref_sync_courses":
                if (sharedPreferences.getBoolean(key, true)) {

                }
                break;
            case "pref_sync_moodle":
                break;
            case "pref_notif_courses":
                break;
        }


    }
}