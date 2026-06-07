package com.hotelos.roomservice.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class OrderItem {

    @NotBlank
    public String name;

    @Min(1)
    public int quantity;

    public BigDecimal unitPrice;

    public OrderItem() {}

    public OrderItem(String name, int quantity, BigDecimal unitPrice) {
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
