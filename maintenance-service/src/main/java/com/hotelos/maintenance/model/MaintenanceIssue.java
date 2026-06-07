package com.hotelos.maintenance.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class MaintenanceIssue {

    private static final AtomicLong SEQ = new AtomicLong(1);

    private final long id;
    private final String roomNumber;
    private final String description;
    private final Urgency urgency;
    private final Instant submittedAt;
    private String assignedTo;
    private boolean resolved;
    private Instant resolvedAt;

    public MaintenanceIssue(String roomNumber, String description, Urgency urgency) {
        this.id = SEQ.getAndIncrement();
        this.roomNumber = roomNumber;
        this.description = description;
        this.urgency = urgency;
        this.submittedAt = Instant.now();
    }

    public long getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public String getDescription() { return description; }
    public Urgency getUrgency() { return urgency; }
    public Instant getSubmittedAt() { return submittedAt; }
    public String getAssignedTo() { return assignedTo; }
    public boolean isResolved() { return resolved; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void assignTo(String technician) { this.assignedTo = technician; }
    public void markResolved() { this.resolved = true; this.resolvedAt = Instant.now(); }
}
