package com.hotelos.reception.service;

import com.hotelos.reception.model.Proximity;
import com.hotelos.reception.model.Room;
import com.hotelos.reception.model.RoomStatus;
import com.hotelos.reception.model.RoomType;
import com.hotelos.reception.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implements the Room Assignment Algorithm (BTEC Task 1.1).
 *
 * Selection criteria, evaluated in this order:
 *   1. Type matches the booked type
 *   2. Status is CLEAN (DIRTY / CLEANING / MAINTENANCE / OCCUPIED excluded)
 *   3. Floor preference: prefer rooms on the requested floor; fall back to any
 *      floor if the preferred floor has none
 *   4. Longest-clean first: among matching rooms, pick the one whose
 *      lastCleanedAt is oldest (fair rotation across the inventory)
 *   5. Proximity preference (LIFT / STAIRS): used only to break ties when
 *      lastCleanedAt is equal
 *
 * Returns Optional.empty() when no room satisfies type + status. Floor and
 * proximity are preferences; they do not exclude rooms.
 */
@Service
public class RoomAssignmentService {

    private final RoomRepository rooms;

    public RoomAssignmentService(RoomRepository rooms) {
        this.rooms = rooms;
    }

    public Optional<Room> assign(RoomType type, Integer floorPreference, Proximity proximityPreference) {
        // Step 1 + 2: hard filters
        List<Room> candidates = rooms.findAll().stream()
                .filter(r -> r.getType() == type)
                .filter(r -> r.getStatus() == RoomStatus.CLEAN)
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Step 3: floor preference — try preferred floor first, then fall back
        if (floorPreference != null) {
            List<Room> onFloor = candidates.stream()
                    .filter(r -> r.getFloor() == floorPreference)
                    .toList();
            if (!onFloor.isEmpty()) {
                candidates = onFloor;
            }
            // else: keep full candidates list (fallback)
        }

        // Step 4: longest-clean first
        // Step 5: proximity tie-break
        Comparator<Room> comparator = Comparator.comparing(Room::getLastCleanedAt);

        if (proximityPreference == Proximity.LIFT) {
            comparator = comparator.thenComparingInt(Room::getMetresFromLift);
        } else if (proximityPreference == Proximity.STAIRS) {
            // Stairs = farthest from lift (inverse of lift)
            comparator = comparator.thenComparing(Comparator.comparingInt(Room::getMetresFromLift).reversed());
        }

        return candidates.stream().min(comparator);
    }
}
