package com.gnut3ll4.syncets.model;

public class MoodleCourse {

    private int id;
    private String shortname;
    private String fullname;
    private int enrolledusercount;
    private String idnumber;
    private int visible;
    private String summary;
    private int summaryformat;
    private String format;
    private boolean showgrades;
    private String lang;
    private boolean enablecompletion;

    public MoodleCourse() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
}
