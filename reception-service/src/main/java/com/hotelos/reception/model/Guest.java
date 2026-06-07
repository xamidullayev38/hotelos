package com.hotelos.reception.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String roomNumber;
    private Instant checkInAt;
    private Instant checkOutAt;
    private int bookedNights;
    private BigDecimal extraCharges = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;

    public Guest() {}

    public Guest(String fullName, String roomNumber, Instant checkInAt, int bookedNights) {
        this.fullName = fullName;
        this.roomNumber = roomNumber;
        this.checkInAt = checkInAt;
        this.bookedNights = bookedNights;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getRoomNumber() { return roomNumber; }
    public Instant getCheckInAt() { return checkInAt; }
    public Instant getCheckOutAt() { return checkOutAt; }
    public int getBookedNights() { return bookedNights; }
    public BigDecimal getExtraCharges() { return extraCharges; }
    public BigDecimal getDiscount() { return discount; }

    public void setCheckOutAt(Instant checkOutAt) { this.checkOutAt = checkOutAt; }
    public void addExtraCharge(BigDecimal amount) { this.extraCharges = this.extraCharges.add(amount); }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
}
