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
import android.widget.LinearLayout;

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

public class PreferenceSyncFragment extends PreferenceFragment {

    SecurePreferences securePreferences;
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
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

        updateLastSync();

        FancyButton syncButton = (FancyButton) view.findViewById(R.id.btn_sync);
        LinearLayout linearLayoutProgress = (LinearLayout) view.findViewById(R.id.linearlayout_progress);

        boolean syncAvailableToday = Utils.isSyncAvailableToday(getActivity());

        if (!syncAvailableToday) {
            syncButton.setEnabled(false);
            syncButton.setVisibility(View.GONE);
            linearLayoutProgress.setVisibility(View.GONE);
        } else {
            syncButton.setEnabled(true);
            syncButton.setVisibility(View.VISIBLE);
        }

        syncButton.setOnClickListener(view1 -> {

                    linearLayoutProgress.setVisibility(View.VISIBLE);
                    syncButton.setVisibility(View.INVISIBLE);

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
                                    syncButton.setVisibility(
                                            Utils.isSyncAvailableToday(getActivity()) ?
                                                    View.VISIBLE : View.GONE);
                                    linearLayoutProgress.setVisibility(View.GONE);
                                    updateLastSync();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    e.printStackTrace();
                                    linearLayoutProgress.setVisibility(View.INVISIBLE);
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

        boolean isSyncCoursesSelected = sharedPreferences.getBoolean("pref_sync_courses", true);
        boolean isSyncMoodleSelected = sharedPreferences.getBoolean("pref_sync_moodle", true);
        boolean isNotificationsActivated = sharedPreferences.getBoolean("pref_notif_courses", true);

        rx.Observable<Object> syncCalendar = Observable.empty();
        rx.Observable<Object> syncMoodle = Observable.empty();

        if (isSyncCoursesSelected) {
            syncCalendar = GoogleCalendarUtils.syncCalendar(getActivity(), isNotificationsActivated)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread());
        }
        if (isSyncMoodleSelected) {
            syncMoodle = GoogleTaskUtils.syncMoodleAssignments(getActivity())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread());
        }

        return Observable.merge(syncCalendar, syncMoodle)
                .flatMap(o -> {
                    Utils.putDate(securePreferences,
                            Constants.LAST_SYNC,
                            new Date(),
                            new GregorianCalendar().getTimeZone());
                    return Observable.empty();
                });
    }
}