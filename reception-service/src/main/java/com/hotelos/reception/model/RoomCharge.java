package com.hotelos.reception.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Charge attached to a room while it is occupied. Created when room-service
 * publishes order.created. Cleared on check-out after the bill is computed.
 */
@Entity
public class RoomCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomNumber;
    private String description;
    private BigDecimal amount;
    private Instant chargedAt;

    public RoomCharge() {}

    public RoomCharge(String roomNumber, String description, BigDecimal amount, Instant chargedAt) {
        this.roomNumber = roomNumber;
        this.description = description;
        this.amount = amount;
        this.chargedAt = chargedAt;
    }

    public Long getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public Instant getChargedAt() { return chargedAt; }
}
