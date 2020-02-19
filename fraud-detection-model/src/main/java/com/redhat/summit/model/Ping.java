package com.redhat.summit.model;

import java.util.Calendar;

public class Ping {
    private String message;

    public Ping() {
        this.message = this.getMessage();
    }

    public String getMessage() {
        return "Ping from Camel @ " + Calendar.getInstance().getTime().toString();
    }
}
