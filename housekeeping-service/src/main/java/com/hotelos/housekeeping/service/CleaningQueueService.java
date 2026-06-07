package com.hotelos.housekeeping.service;

import com.hotelos.common.broker.BrokerClient;
import com.hotelos.common.events.Topics;
import com.hotelos.housekeeping.model.RoomStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Maintains the FIFO cleaning queue. Subscribes to room.vacated and enqueues
 * the vacated room. Receptionists never have to remember to dispatch to
 * housekeeping — the broker delivers the event automatically.
 *
 * Thread safety: the queue is a ConcurrentLinkedDeque because the broker
 * client receives messages on its own thread while controller methods read
 * /mark-clean on Tomcat threads.
 */
@Service
public class CleaningQueueService {

    private final BrokerClient broker;
    private final Deque<String> queue = new ConcurrentLinkedDeque<>();
    private final Map<String, RoomStatus> roomStates = new java.util.concurrent.ConcurrentHashMap<>();

    public CleaningQueueService(BrokerClient broker) {
        this.broker = broker;
    }

    @PostConstruct
    void subscribe() {
        broker.subscribe(Topics.ROOM_VACATED, payload -> {
            String roomNumber = String.valueOf(payload.get("roomNumber"));
            enqueue(roomNumber);
        });
    }

    public void enqueue(String roomNumber) {
        if (!queue.contains(roomNumber)) {
            queue.offer(roomNumber);
            roomStates.put(roomNumber, RoomStatus.DIRTY);
            System.out.println("[housekeeping] queued " + roomNumber);
        }
    }

    public boolean startCleaning(String roomNumber) {
        if (!queue.contains(roomNumber)) return false;
        roomStates.put(roomNumber, RoomStatus.CLEANING);
        broker.publish(Topics.ROOM_STATUS_CHANGED, Map.of(
                "roomNumber", roomNumber,
                "status", RoomStatus.CLEANING.name()
        ));
        return true;
    }

    public boolean markClean(String roomNumber) {
        boolean removed = queue.remove(roomNumber);
        roomStates.put(roomNumber, RoomStatus.CLEAN);
        broker.publish(Topics.ROOM_STATUS_CHANGED, Map.of(
                "roomNumber", roomNumber,
                "status", RoomStatus.CLEAN.name()
        ));
        return removed;
    }

    public LinkedList<String> snapshot() {
        return new LinkedList<>(queue);
    }
}
