package com.hotelos.roomservice.model;

public enum OrderStatus {
    RECEIVED, PREPARING, DELIVERING, DELIVERED;

    /** Returns the next status in the lifecycle, or null at DELIVERED. */
    public OrderStatus next() {
        return switch (this) {
            case RECEIVED   -> PREPARING;
            case PREPARING  -> DELIVERING;
            case DELIVERING -> DELIVERED;
            case DELIVERED  -> null;
        };
    }
}
