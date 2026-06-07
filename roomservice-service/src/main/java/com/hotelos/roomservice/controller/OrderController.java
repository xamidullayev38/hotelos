package com.hotelos.roomservice.controller;

import com.hotelos.roomservice.model.Order;
import com.hotelos.roomservice.model.OrderItem;
import com.hotelos.roomservice.service.OrderQueueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@Validated
public class OrderController {

    private final OrderQueueService orders;

    public OrderController(OrderQueueService orders) {
        this.orders = orders;
    }

    public static class CreateOrderRequest {
        @Pattern(regexp = "^[1-2][0-9]{2}$", message = "roomNumber must be 100-299")
        public String roomNumber;

        @NotEmpty
        @Valid
        public List<OrderItem> items;
    }

    @GetMapping("/orders")
    public Collection<Order> list() {
        return orders.all();
    }

    @PostMapping("/orders")
    public Order create(@Valid @RequestBody CreateOrderRequest req) {
        return orders.create(req.roomNumber, req.items);
    }

    @PostMapping("/orders/{id}/advance")
    public ResponseEntity<?> advance(@PathVariable long id) {
        Order order = orders.advance(id);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "ORDER_NOT_FOUND", "message", "No order with id " + id
            ));
        }
        return ResponseEntity.ok(order);
    }
}
