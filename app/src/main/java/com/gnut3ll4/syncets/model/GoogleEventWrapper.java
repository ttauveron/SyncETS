package com.gnut3ll4.syncets.model;

import com.google.api.services.calendar.model.Event;

public class GoogleEventWrapper {

    private Event event;

    public GoogleEventWrapper(Event event) {
        this.event = event;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GoogleEventWrapper &&
                ((GoogleEventWrapper) o).event.getId().equals(this.event.getId());

    }

    public Event getEvent() {
        return event;
    }
}