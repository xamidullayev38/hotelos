package com.hotelos.roomservice.service;

import com.hotelos.common.broker.BrokerClient;
import com.hotelos.common.events.Topics;
import com.hotelos.roomservice.model.Order;
import com.hotelos.roomservice.model.OrderItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIFO queue of room-service orders. The "kitchen" advances each order
 * through the RECEIVED → PREPARING → DELIVERING → DELIVERED lifecycle by
 * calling advance(). Every transition is broadcast on the broker so the
 * dashboard reflects it without polling.
 */
@Service
public class OrderQueueService {

    private final BrokerClient broker;
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();

    public OrderQueueService(BrokerClient broker) {
        this.broker = broker;
    }

    public Order create(String roomNumber, List<OrderItem> items) {
        Order order = new Order(roomNumber, items);
        orders.put(order.getId(), order);

        broker.publish(Topics.ORDER_CREATED, Map.of(
                "orderId", order.getId(),
                "roomNumber", order.getRoomNumber(),
                "total", order.getTotal(),
                "itemCount", items.size(),
                "status", order.getStatus().name()
        ));
        return order;
    }

    public Order advance(long orderId) {
        Order order = orders.get(orderId);
        if (order == null) return null;
        if (!order.advance()) return order; // already DELIVERED

        broker.publish(Topics.ORDER_STATUS_CHANGED, Map.of(
                "orderId", order.getId(),
                "roomNumber", order.getRoomNumber(),
                "status", order.getStatus().name()
        ));
        return order;
    }

    public Collection<Order> all() {
        return new ArrayList<>(orders.values());
    }
}
