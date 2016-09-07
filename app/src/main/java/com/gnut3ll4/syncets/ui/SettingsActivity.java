package com.gnut3ll4.syncets.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.gnut3ll4.syncets.ApplicationManager;
import com.gnut3ll4.syncets.R;
import com.gnut3ll4.syncets.service.DailyListener;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getFragmentManager().beginTransaction().replace(R.id.flContent, new MyPreferenceFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ApplicationManager.userCredentials == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, 0);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_exit_to_app_black_48dp)
                        .setTitle(R.string.logout)
                        .setMessage(R.string.confirm_logout)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ApplicationManager.logout(SettingsActivity.this);
                            }

                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

}