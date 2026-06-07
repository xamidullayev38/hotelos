package com.hotelos.reception.controller;

import com.hotelos.common.broker.BrokerClient;
import com.hotelos.common.events.Topics;
import com.hotelos.reception.dto.CheckInRequest;
import com.hotelos.reception.dto.CheckInResponse;
import com.hotelos.reception.dto.CheckOutResponse;
import com.hotelos.reception.model.Guest;
import com.hotelos.reception.model.Room;
import com.hotelos.reception.model.RoomCharge;
import com.hotelos.reception.model.RoomStatus;
import com.hotelos.reception.repository.GuestRepository;
import com.hotelos.reception.repository.RoomChargeRepository;
import com.hotelos.reception.repository.RoomRepository;
import com.hotelos.reception.service.BillingService;
import com.hotelos.reception.service.RoomAssignmentService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class ReceptionController {

    private final RoomAssignmentService assignment;
    private final BillingService billing;
    private final RoomRepository rooms;
    private final GuestRepository guests;
    private final RoomChargeRepository charges;
    private final BrokerClient broker;

    public ReceptionController(RoomAssignmentService assignment,
                               BillingService billing,
                               RoomRepository rooms,
                               GuestRepository guests,
                               RoomChargeRepository charges,
                               BrokerClient broker) {
        this.assignment = assignment;
        this.billing = billing;
        this.rooms = rooms;
        this.guests = guests;
        this.charges = charges;
        this.broker = broker;
    }

    @PostConstruct
    void subscribe() {
        // Charge room-service orders to the open guest in that room
        broker.subscribe(Topics.ORDER_CREATED, payload -> {
            String roomNumber = String.valueOf(payload.get("roomNumber"));
            Number totalNum = (Number) payload.get("total");
            if (roomNumber == null || totalNum == null) return;
            BigDecimal amount = new BigDecimal(totalNum.toString());
            charges.save(new RoomCharge(roomNumber, "Room service order", amount, Instant.now()));
        });

        // Sync room status when housekeeping flips it
        broker.subscribe(Topics.ROOM_STATUS_CHANGED, payload -> {
            String roomNumber = String.valueOf(payload.get("roomNumber"));
            String statusName = String.valueOf(payload.get("status"));
            rooms.findById(roomNumber).ifPresent(r -> {
                RoomStatus status = RoomStatus.valueOf(statusName);
                r.setStatus(status);
                if (status == RoomStatus.CLEAN) {
                    r.setLastCleanedAt(Instant.now());
                }
                rooms.save(r);
            });
        });

        // Bring room back from MAINTENANCE -> DIRTY when an issue resolves
        broker.subscribe(Topics.MAINTENANCE_RESOLVED, payload -> {
            String roomNumber = String.valueOf(payload.get("roomNumber"));
            rooms.findById(roomNumber).ifPresent(r -> {
                if (r.getStatus() == RoomStatus.MAINTENANCE) {
                    r.setStatus(RoomStatus.DIRTY);
                    rooms.save(r);
                }
            });
        });
    }

    @GetMapping("/rooms")
    public List<Room> listRooms() {
        return rooms.findAll();
    }

    /**
     * Lock for the check-in path. The original code raced under concurrent
     * requests: two callers could both observe the same room as CLEAN, both
     * receive it as the assignment result, and both save it as OCCUPIED — one
     * guest's record then silently shared a room. Found via TS-06 (see
     * docs/report/debug-log.md, BUG-01). Single in-process lock is fine here
     * because there is exactly one Reception instance.
     */
    private final Object checkInLock = new Object();

    @PostMapping("/checkin")
    @Transactional
    public ResponseEntity<?> checkIn(@Valid @RequestBody CheckInRequest req) {
        synchronized (checkInLock) {
            return doCheckIn(req);
        }
    }

    private ResponseEntity<?> doCheckIn(CheckInRequest req) {
        // Run the room assignment algorithm
        Optional<Room> assigned = assignment.assign(req.roomType, req.floorPreference, req.proximityPreference);

        if (assigned.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "ROOMS_UNAVAILABLE",
                    "message", "No clean rooms of type " + req.roomType + " available right now.",
                    "suggestion", "Try a different room type or join the wait list."
            ));
        }

        Room room = assigned.get();
        room.setStatus(RoomStatus.OCCUPIED);
        rooms.save(room);

        Guest guest = guests.save(new Guest(req.fullName, room.getNumber(), Instant.now(), req.nights));

        broker.publish(Topics.ROOM_STATUS_CHANGED, Map.of(
                "roomNumber", room.getNumber(),
                "status", RoomStatus.OCCUPIED.name(),
                "guestName", guest.getFullName()
        ));

        return ResponseEntity.ok(new CheckInResponse(
                guest.getId(),
                room.getNumber(),
                "Welcome, " + guest.getFullName() + ". Room " + room.getNumber() + " on floor " + room.getFloor() + "."
        ));
    }

    @PostMapping("/checkout/{roomNumber}")
    @Transactional
    public ResponseEntity<?> checkOut(@PathVariable String roomNumber) {
        Optional<Room> roomOpt = rooms.findById(roomNumber);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "ROOM_NOT_FOUND", "message", "Room " + roomNumber + " does not exist."
            ));
        }
        Room room = roomOpt.get();

        Optional<Guest> guestOpt = guests.findFirstByRoomNumberAndCheckOutAtIsNull(roomNumber);
        if (guestOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "NO_OPEN_GUEST", "message", "Room " + roomNumber + " has no open check-in."
            ));
        }
        Guest guest = guestOpt.get();

        Instant now = Instant.now();
        CheckOutResponse bill = billing.computeBill(guest, room, now);

        guest.setCheckOutAt(now);
        guests.save(guest);

        room.setStatus(RoomStatus.DIRTY);
        rooms.save(room);

        // Clear charges so they don't double-bill on a future stay
        charges.deleteByRoomNumber(roomNumber);

        // Announce vacancy — housekeeping will pick it up
        broker.publish(Topics.ROOM_VACATED, Map.of(
                "roomNumber", roomNumber,
                "vacatedAt", now.toString()
        ));
        broker.publish(Topics.ROOM_STATUS_CHANGED, Map.of(
                "roomNumber", roomNumber,
                "status", RoomStatus.DIRTY.name()
        ));

        return ResponseEntity.ok(bill);
    }
}
