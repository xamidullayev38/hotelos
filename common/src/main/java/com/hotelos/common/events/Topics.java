package com.hotelos.common.events;

/**
 * Centralised list of broker topic names. Keeping them in one place prevents
 * typos in publish/subscribe calls — a hard-to-debug class of bug in pub/sub
 * systems where a misspelled topic silently delivers no messages.
 */
public final class Topics {

    public static final String ROOM_VACATED          = "room.vacated";
    public static final String ROOM_STATUS_CHANGED   = "room.status_changed";
    public static final String ORDER_CREATED         = "order.created";
    public static final String ORDER_STATUS_CHANGED  = "order.status_changed";
    public static final String MAINTENANCE_REPORTED  = "maintenance.reported";
    public static final String MAINTENANCE_RESOLVED  = "maintenance.resolved";

    private Topics() {}
}
