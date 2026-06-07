package com.hotelos.roomservice.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Room-service order. Encapsulates its own status transition rule via
 * {@link #advance()} — the queue can call it without knowing the OrderStatus
 * enum's internal lifecycle. Demonstrates encapsulation (Task 2.3).
 */
public class Order {

    private static final AtomicLong SEQ = new AtomicLong(1);

    private final long id;
    private final String roomNumber;
    private final List<OrderItem> items;
    private final BigDecimal total;
    private final Instant createdAt;
    private OrderStatus status;

    public Order(String roomNumber, List<OrderItem> items) {
        this.id = SEQ.getAndIncrement();
        this.roomNumber = roomNumber;
        this.items = List.copyOf(items);
        this.total = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        this.createdAt = Instant.now();
        this.status = OrderStatus.RECEIVED;
    }

    public boolean advance() {
        OrderStatus next = status.next();
        if (next == null) return false;
        this.status = next;
        return true;
    }

    public long getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
}
