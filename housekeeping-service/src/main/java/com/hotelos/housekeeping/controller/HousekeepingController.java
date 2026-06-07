package com.hotelos.housekeeping.controller;

import com.hotelos.housekeeping.service.CleaningQueueService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.Map;

@RestController
@Validated
public class HousekeepingController {

    private static final String ROOM_PATTERN = "^[1-2][0-9]{2}$"; // 100-299

    private final CleaningQueueService queueService;

    public HousekeepingController(CleaningQueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/cleaning-queue")
    public LinkedList<String> queue() {
        return queueService.snapshot();
    }

    @PostMapping("/start-cleaning/{roomNumber}")
    public ResponseEntity<?> startCleaning(@PathVariable @Pattern(regexp = ROOM_PATTERN) String roomNumber) {
        boolean ok = queueService.startCleaning(roomNumber);
        if (!ok) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "ROOM_NOT_QUEUED", "message", "Room " + roomNumber + " is not in the cleaning queue."
            ));
        }
        return ResponseEntity.ok(Map.of("roomNumber", roomNumber, "status", "CLEANING"));
    }

    @PostMapping("/mark-clean/{roomNumber}")
    public ResponseEntity<?> markClean(@PathVariable @Pattern(regexp = ROOM_PATTERN) String roomNumber) {
        queueService.markClean(roomNumber);
        return ResponseEntity.ok(Map.of("roomNumber", roomNumber, "status", "CLEAN"));
    }
}
