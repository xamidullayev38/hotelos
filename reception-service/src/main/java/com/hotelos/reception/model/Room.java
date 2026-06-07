package com.hotelos.reception.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persistent room record. Field access is via getters; setters keep the
 * entity mutable for JPA but invariants on status transitions live in the
 * services that own them (Reception, Housekeeping).
 */
@Entity
public class Room {

    @Id
    private String number;

    private int floor;

    @Enumerated(EnumType.STRING)
    private RoomType type;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    /** Distance to the lift in metres. Used as proximity tie-breaker. */
    private int metresFromLift;

    private BigDecimal nightlyRate;

    /** Last time this room was marked CLEAN. Drives the fair-rotation rule. */
    private Instant lastCleanedAt;

    public Room() {}

    public Room(String number, int floor, RoomType type, RoomStatus status,
                int metresFromLift, BigDecimal nightlyRate, Instant lastCleanedAt) {
        this.number = number;
        this.floor = floor;
        this.type = type;
        this.status = status;
        this.metresFromLift = metresFromLift;
        this.nightlyRate = nightlyRate;
        this.lastCleanedAt = lastCleanedAt;
    }

    public String getNumber() { return number; }
    public int getFloor() { return floor; }
    public RoomType getType() { return type; }
    public RoomStatus getStatus() { return status; }
    public int getMetresFromLift() { return metresFromLift; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
    public Instant getLastCleanedAt() { return lastCleanedAt; }

    public void setStatus(RoomStatus status) { this.status = status; }
    public void setLastCleanedAt(Instant lastCleanedAt) { this.lastCleanedAt = lastCleanedAt; }
}
