package com.hotelos.reception.dto;

import java.math.BigDecimal;
import java.util.List;

public class CheckOutResponse {
    public String guestName;
    public String roomNumber;
    public int nights;
    public BigDecimal roomTotal;
    public BigDecimal chargesTotal;
    public BigDecimal extras;
    public BigDecimal discount;
    public BigDecimal grandTotal;
    public List<LineItem> chargeLines;

    public static class LineItem {
        public String description;
        public BigDecimal amount;
        public LineItem(String description, BigDecimal amount) {
            this.description = description;
            this.amount = amount;
        }
    }
}
