package com.gnut3ll4.syncets.model;

import android.text.TextUtils;

import com.securepreferences.SecurePreferences;

public class UserCredentials {
    public static final String CODE_U = "codeAccesUniversel";
    public static final String CODE_P = "motPasse";
    public static final String MOODLE_TOKEN = "moodleToken";

    private String password = "";
    private String username = "";
    private String moodleToken = "";

    public UserCredentials(final SecurePreferences prefs) {
        if (prefs != null) {
            username = prefs.getString(UserCredentials.CODE_P, "");
            password = prefs.getString(UserCredentials.CODE_U, "");
            moodleToken = prefs.getString(UserCredentials.MOODLE_TOKEN, "");
        }
    }

    public UserCredentials(final String codeU, final String codeP) {
        username = codeU;
        password = codeP;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getMoodleToken() {
        return moodleToken;
    }

    public void setMoodleToken(String moodleToken) {
        this.moodleToken = moodleToken;
    }

    public boolean isLoggedIn() {
        return !TextUtils.isEmpty(password) && !TextUtils.isEmpty(username);
    }

}