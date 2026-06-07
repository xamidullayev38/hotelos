package com.hotelos.reception.dto;

public class CheckInResponse {
    public Long guestId;
    public String roomNumber;
    public String message;

    public CheckInResponse(Long guestId, String roomNumber, String message) {
        this.guestId = guestId;
        this.roomNumber = roomNumber;
        this.message = message;
    }
}
