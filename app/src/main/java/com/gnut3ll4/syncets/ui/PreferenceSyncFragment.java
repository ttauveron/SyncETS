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
import com.gnut3ll4.syncets.ApplicationManager;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.utils.Constants;
import com.gnut3ll4.syncets.utils.GoogleCalendarUtils;
import com.gnut3ll4.syncets.utils.GoogleTaskUtils;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.tasks.TasksScopes;
import com.securepreferences.SecurePreferences;

import java.util.Arrays;

import mehdi.sakout.fancybuttons.FancyButton;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PreferenceSyncFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        //todo last sync display

        //todo if first login
        onCoachMark();
    }

    public void onCoachMark() {

        //todo add ok button
        final Dialog dialog = new Dialog(getActivity(), R.style.WalkthroughTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.coach_mark);
        dialog.setCanceledOnTouchOutside(true);
        //for dismissing anywhere you touch
        View masterView = dialog.findViewById(R.id.coach_mark_master_view);
        View button = dialog.findViewById(R.id.btn_sync_overlay);
        button.setEnabled(false);
        View.OnClickListener dismissOnClick = view -> dialog.dismiss();
        masterView.setOnClickListener(dismissOnClick);
        button.setOnClickListener(dismissOnClick);
        dialog.show();
        //todo E/WindowManager: android.view.WindowLeaked when logout
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, null);

        Preference pref = findPreference("pref_static_field_key");

        //Update the summary with user input data
        pref.setSummary("Never");

        FancyButton syncButton = (FancyButton) view.findViewById(R.id.btn_sync);
        CircularProgressView circularProgressView = (CircularProgressView) view.findViewById(R.id.progress_view);

        syncButton.setOnClickListener(view1 -> {

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
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<Object>() {
                                @Override
                                public void onCompleted() {
                                    Log.d("SYNCETS", "UI sync ended");
                                    //todo add animation translate here
                                    circularProgressView.setVisibility(View.GONE);
                                    syncButton.setVisibility(View.GONE);
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

    public rx.Observable<Object> getSyncObservable() {


        rx.Observable<Object> syncCalendarEnded = GoogleCalendarUtils.syncCalendar(getActivity(), true);
        rx.Observable<Object> syncMoodleEnded = GoogleTaskUtils.syncMoodleAssignments(getActivity());
        return rx.Observable.zip(syncCalendarEnded, syncMoodleEnded, (o, o2) -> rx.Observable.empty());
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