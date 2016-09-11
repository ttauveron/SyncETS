package com.gnut3ll4.syncets.utils;

import android.content.Context;

import com.gnut3ll4.syncets.ApplicationManager;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.model.MoodleAssignment;
import com.gnut3ll4.syncets.model.MoodleCourse;
import com.gnut3ll4.syncets.ui.LoginActivity;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.securepreferences.SecurePreferences;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import rx.Observable;

public class GoogleTaskUtils {

    public static Observable<Task> getMoodleAssignmentsTaskEvents() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://ena.etsmtl.ca/login/token.php?username=" + ApplicationManager.userCredentials.getUsername() +
                        "&password=" + ApplicationManager.userCredentials.getPassword() + "&service=moodle_mobile_app")
                .get()
                .build();

        try {
            return Observable.just(client.newCall(request).execute())
                    .flatMap(response -> {
                        try {

                            JSONObject jsonObject = new JSONObject(response.body().string());
                            String token = jsonObject.getString("token");
                            ApplicationManager.userCredentials.setMoodleToken(token);


                            Request request2 = new Request.Builder()
                                    .url("https://ena.etsmtl.ca/webservice/rest/server.php?moodlewsrestformat=json&wstoken=" + token + "&wsfunction=moodle_webservice_get_siteinfo")
                                    .get()
                                    .build();

                            return Observable.just(client.newCall(request2).execute());
                        } catch (Exception e) {
                            return Observable.error(e);
                        }
                    })
                    .flatMap(response1 -> {
                        try {
                            JSONObject jsonObject = new JSONObject(response1.body().string());
                            int userid = jsonObject.getInt("userid");

                            Request request3 = new Request.Builder()
                                    .url("https://ena.etsmtl.ca/webservice/rest/server.php?moodlewsrestformat=json&wstoken=" +
                                            ApplicationManager.userCredentials.getMoodleToken() +
                                            "&wsfunction=moodle_enrol_get_users_courses&userid=" + userid)
                                    .get()
                                    .build();

                            return Observable.just(client.newCall(request3).execute());
                        } catch (Exception e) {
                            return Observable.error(e);
                        }
                    })
                    .flatMap(response2 -> {
                        try {
                            List<MoodleCourse> moodleCourses = new Gson().fromJson(
                                    response2.body().string(),
                                    new TypeToken<List<MoodleCourse>>() {
                                    }.getType());

                            return Observable.from(moodleCourses);
                        } catch (Exception e) {
                            return Observable.error(e);
                        }

                    })
                    .filter(moodleCourse -> {
                        return moodleCourse.getShortname().startsWith("S" + Utils.getCurrentCodeSession());
                    })
                    .toList()
                    .flatMap(moodleCourses1 -> {

                        if (moodleCourses1.isEmpty()) {
                            return Observable.error(new Exception("No Moodle courses found"));
                        }

                        String listCourses = "";
                        for (int i = 0; i < moodleCourses1.size(); i++) {
                            listCourses += "&courseids%5B%5D=" + moodleCourses1.get(i).getId();
                        }


                        Request request4 = new Request.Builder()
                                .url("https://ena.etsmtl.ca/webservice/rest/server.php?moodlewsrestformat=json&" +
                                        "wstoken=" + ApplicationManager.userCredentials.getMoodleToken() + "&" +
                                        "wsfunction=mod_assign_get_assignments&" +
                                        "courseids%5B%5D=" + listCourses)
                                .get()
                                .build();

                        try {
                            return Observable.just(client.newCall(request4).execute());
                        } catch (Exception e) {
                            return Observable.error(e);
                        }
                    })
                    .flatMap(response3 -> {
                        try {

                            String body = response3.body().string();
                            JSONObject jsonObject = new JSONObject(body);
                            JSONArray jsonArrayCourses = jsonObject.getJSONArray("courses");
                            List<MoodleAssignment> moodleAssignments = new ArrayList<MoodleAssignment>();
                            for (int i = 0; i < jsonArrayCourses.length(); i++) {
                                JSONArray jsonArrayAssignments = jsonArrayCourses.getJSONObject(i).getJSONArray("assignments");

                                moodleAssignments.addAll(new Gson()
                                        .fromJson(
                                                jsonArrayAssignments.toString(),
                                                new TypeToken<List<MoodleAssignment>>() {
                                                }.getType()));
                            }


                            return Observable.from(moodleAssignments);
                        } catch (JSONException e) {
                            return Observable.empty();
                        } catch (IOException e) {
                            return Observable.error(e);
                        }
                    })
                    .flatMap(moodleAssignment -> {
                        Task task = new Task();

                        String encodedId = BaseEncoding.base32Hex()
                                .encode(("moodleassignment" + moodleAssignment.getId()).getBytes());
//                        task.setId(encodedId);
                        task.setTitle(moodleAssignment.getName());


                        TimeZone mTimeZone = new GregorianCalendar().getTimeZone();
                        int mGMTOffset = mTimeZone.getRawOffset();

                        task.setDue(new com.google.api.client.util.DateTime(moodleAssignment.getDuedate() * 1000));
                        return Observable.just(task);
                    });
        } catch (IOException e) {
            return Observable.error(e);
        }
    }


    /**
     * Deletes the tasklist with the specified name if it exists and creates a new tasklist with the specified name
     *
     * @param tasksClient
     * @param taskListName
     * @return id of the tasklist
     * @throws IOException
     */
    public static String createETSTaskListId(Tasks tasksClient, String taskListName) throws IOException {
        String taskListId = "";

        //If tasklist exists on Google, we delete it.
        List<TaskList> items = tasksClient.tasklists().list().execute().getItems();
        for (TaskList item : items) {
            if (item.getTitle().equals(taskListName)) {
                tasksClient.tasklists().delete(item.getId()).execute();
            }
        }

        //Then we create the new one.
        TaskList taskList = new TaskList();
        taskList.setTitle(taskListName);
        taskListId = tasksClient.tasklists().insert(taskList).execute().getId();

        return taskListId;
    }

    private static String tasklistId;

    public static Observable<Object> syncMoodleAssignments(Context context) {

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential;
        SecurePreferences securePreferences = new SecurePreferences(context);

        String selectedAccount = securePreferences.getString(Constants.SELECTED_ACCOUNT, "");

        credential = LoginActivity.mCredential;

        Tasks taskClient = new Tasks.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("SyncETS")
                .build();

        //Checking if tasklist exists and create it if not
        if (!selectedAccount.isEmpty()) {
            try {
                tasklistId = GoogleTaskUtils.createETSTaskListId(taskClient,
                        context.getResources().getString(R.string.ets_tasklist));
            } catch (IOException e) {
                Observable.error(e);
            }

        }


        //Sync Moodle Assignment in Google Task
        return GoogleTaskUtils.getMoodleAssignmentsTaskEvents()
                .flatMap(task -> {
                    try {
                        taskClient.tasks().insert(tasklistId, task).execute();
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                    return Observable.empty();
                });


//                .subscribeOn(Schedulers.newThread())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Observer<Object>() {
//                    @Override
//                    public void onCompleted() {
//                        Log.d("SYNCETS", "Moodle sync ended");
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//
//                    }
//
//                    @Override
//                    public void onNext(Object o) {
//
//                    }
//                });
    }

}
