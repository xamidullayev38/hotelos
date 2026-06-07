package com.hotelos.reception.service;

import com.hotelos.reception.dto.CheckOutResponse;
import com.hotelos.reception.model.Guest;
import com.hotelos.reception.model.Room;
import com.hotelos.reception.model.RoomCharge;
import com.hotelos.reception.repository.RoomChargeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Implements the Billing Algorithm (BTEC Task 1.2).
 *
 *   total = (actualNights × room.nightlyRate)
 *         + Σ orderCharges (room-service items charged during stay)
 *         + extras (minibar, late checkout, etc.)
 *         − discount (capped at subtotal so a refund is never produced)
 *
 * Edge cases handled:
 *   - Early check-out: actualNights computed from check-in/now, never zero
 *   - Zero orders: chargesTotal == 0, no error
 *   - Discount > subtotal: discount capped at subtotal
 *
 * BigDecimal throughout — never float/double for currency.
 */
@Service
public class BillingService {

    private final RoomChargeRepository charges;

    public BillingService(RoomChargeRepository charges) {
        this.charges = charges;
    }

    public CheckOutResponse computeBill(Guest guest, Room room, Instant checkOutMoment) {
        // Step 1: actual nights stayed (at least 1)
        int actualNights = Math.max(1, nightsBetween(guest.getCheckInAt(), checkOutMoment));

        // Step 2: room subtotal
        BigDecimal roomTotal = room.getNightlyRate()
                .multiply(BigDecimal.valueOf(actualNights))
                .setScale(2, RoundingMode.HALF_UP);

        // Step 3: sum of room-service charges
        List<RoomCharge> roomCharges = charges.findByRoomNumber(room.getNumber());
        BigDecimal chargesTotal = roomCharges.stream()
                .map(RoomCharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Step 4: extras already accumulated on the guest record
        BigDecimal extras = guest.getExtraCharges().setScale(2, RoundingMode.HALF_UP);

        // Step 5: cap the discount
        BigDecimal subtotal = roomTotal.add(chargesTotal).add(extras);
        BigDecimal discount = guest.getDiscount().min(subtotal).setScale(2, RoundingMode.HALF_UP);

        BigDecimal grandTotal = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);

        CheckOutResponse response = new CheckOutResponse();
        response.guestName = guest.getFullName();
        response.roomNumber = room.getNumber();
        response.nights = actualNights;
        response.roomTotal = roomTotal;
        response.chargesTotal = chargesTotal;
        response.extras = extras;
        response.discount = discount;
        response.grandTotal = grandTotal;
        response.chargeLines = roomCharges.stream()
                .map(c -> new CheckOutResponse.LineItem(c.getDescription(), c.getAmount()))
                .toList();
        return response;
    }

    private static int nightsBetween(Instant checkIn, Instant checkOut) {
        long hours = Duration.between(checkIn, checkOut).toHours();
        // any partial day counts as a night
        return (int) Math.max(0, Math.ceil(hours / 24.0));
    }
}
