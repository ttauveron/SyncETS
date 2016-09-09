package com.gnut3ll4.syncets.model;

public class MoodleAssignment {

    private int id;
    private String name;
    private long duedate;

    public MoodleAssignment() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDuedate() {
        return duedate;
    }

    public void setDuedate(long duedate) {
        this.duedate = duedate;
    }
}
