package com.gnut3ll4.syncets.ui;


import android.Manifest;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.gnut3ll4.signetswebservices.soap.IServiceEvents;
import com.gnut3ll4.signetswebservices.soap.OperationResult;
import com.gnut3ll4.signetswebservices.soap.SignetsMobileSoap;
import com.gnut3ll4.syncets.ApplicationManager;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.model.UserCredentials;
import com.gnut3ll4.syncets.service.DailyListener;
import com.gnut3ll4.syncets.utils.Constants;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;
import com.securepreferences.SecurePreferences;

import java.io.IOException;
import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.AfterPermissionGranted;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;


    @Bind(R.id.input_email)
    EditText _emailText;
    @Bind(R.id.input_password)
    EditText _passwordText;
    @Bind(R.id.btn_login)
    Button _loginButton;
    private UserCredentials userCredentials;
    private SecurePreferences securePreferences;

    private static final String[] SCOPES = {CalendarScopes.CALENDAR, TasksScopes.TASKS};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        securePreferences = new SecurePreferences(this);

//        ApplicationManager.googleAccountCredential = GoogleAccountCredential.usingOAuth2(
//                getApplicationContext(), Arrays.asList(SCOPES))
//                .setBackOff(new ExponentialBackOff());

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });


        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.GET_ACCOUNTS)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.GET_ACCOUNTS},
                        1616);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

    }

    public void login() {
        Log.d(TAG, "Login");

//        if (!validate()) {
//            onLoginFailed();
//            return;
//        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        userCredentials = new UserCredentials(email, password);

        SignetsMobileSoap signetsMobileSoap = new SignetsMobileSoap(new IServiceEvents() {
            @Override
            public void Starting() {

            }

            @Override
            public void Completed(OperationResult result) {
                progressDialog.dismiss();
                if (result.Result instanceof Boolean) {
                    Boolean isLoginValid = (Boolean) result.Result;

                    if (isLoginValid) {
                        onLoginSuccess();
                        return;
                    }
                }
                onLoginFailed();
            }
        });

        signetsMobileSoap.donneesAuthentificationValidesAsync(userCredentials.getUsername(), userCredentials.getPassword());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1616) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.e("persmission granted", "true");
                // permission was granted, yay! Do the
                // contacts-related task you need to do.

            } else {
                Log.e("persmission granted", "false");

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Constants.REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            securePreferences.edit().putString(Constants.SELECTED_ACCOUNT, accountName).commit();
        }

        if (requestCode == REQUEST_AUTHORIZATION) {

        }

        if (requestCode == REQUEST_ACCOUNT_PICKER && resultCode == RESULT_OK && data != null &&
                data.getExtras() != null) {
            String accountName =
                    data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (accountName != null) {
                securePreferences.edit().putString(Constants.SELECTED_ACCOUNT, accountName).apply();
                GoogleAccountCredential googleCredentials = ApplicationManager.getGoogleCredentials(this);


                HttpTransport transport = AndroidHttp.newCompatibleTransport();
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                Calendar calendar = new Calendar.Builder(transport, jsonFactory, googleCredentials)
                        .setApplicationName("SyncETS")
                        .build();
                Tasks taskClient = new Tasks.Builder(
                        transport, jsonFactory, googleCredentials)
                        .setApplicationName("SyncETS")
                        .build();

                new AsyncTask<Void, Void, String>() {
                    protected String doInBackground(Void... params) {
                        try {
                            calendar.calendarList().list().execute().getItems();
                            taskClient.tasklists().list().execute().getItems();

                            WakefulIntentService.scheduleAlarms(new DailyListener(), getApplicationContext(), false);

                            startActivity(new Intent(LoginActivity.this, SettingsActivity.class));
                            finish();
                        } catch (UserRecoverableAuthIOException e) {
                            startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    protected void onPostExecute(String msg) {
                        // Post Code
                        // Use `msg` in code
                    }
                }.execute();


            }
        }
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(true);

        ApplicationManager.userCredentials = userCredentials;

        securePreferences.edit().putString(userCredentials.CODE_U, userCredentials.getUsername()).commit();
        securePreferences.edit().putString(userCredentials.CODE_P, userCredentials.getPassword()).commit();

        selectAccount();
    }

    private void selectAccount() {

//        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
//                new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
//        startActivityForResult(intent, Constants.REQUEST_CODE_EMAIL);
        chooseAccount();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

//    public boolean validate() {
//        boolean valid = true;
//
//        String email = _emailText.getText().toString();
//        String password = _passwordText.getText().toString();
//
//        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            _emailText.setError("enter a valid email address");
//            valid = false;
//        } else {
//            _emailText.setError(null);
//        }
//
//        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
//            _passwordText.setError("between 4 and 10 alphanumeric characters");
//            valid = false;
//        } else {
//            _passwordText.setError(null);
//        }
//
//        return valid;
//    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
//        if (EasyPermissions.hasPermissions(
//                this, Manifest.permission.GET_ACCOUNTS)) {
//                // Start a dialog from which the user can choose an account
        GoogleAccountCredential googleAccountCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        startActivityForResult(
                googleAccountCredential.newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
//        } else {
//            // Request the GET_ACCOUNTS permission via a user dialog
//            EasyPermissions.requestPermissions(
//                    this,
//                    "This app needs to access your Google account (via Contacts).",
//                    REQUEST_PERMISSION_GET_ACCOUNTS,
//                    Manifest.permission.GET_ACCOUNTS);
//        }
    }
}