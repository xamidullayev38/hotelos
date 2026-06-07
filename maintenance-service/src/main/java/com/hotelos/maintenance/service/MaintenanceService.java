package com.hotelos.maintenance.service;

import com.hotelos.common.broker.BrokerClient;
import com.hotelos.common.events.Topics;
import com.hotelos.maintenance.model.MaintenanceIssue;
import com.hotelos.maintenance.model.Urgency;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates the priority queue and the technician roster. Brings in two
 * collaborating data structures:
 *
 *   - {@link MaintenancePriorityQueue} for ordering of pending issues
 *   - A round-robin {@link Deque} of available technicians for assignment
 *
 * A separate map keeps every issue ever seen, so resolve() can find one whose
 * priority-queue entry has already been polled.
 */
@Service
public class MaintenanceService {

    private final BrokerClient broker;
    private final MaintenancePriorityQueue queue = new MaintenancePriorityQueue();
    private final Map<Long, MaintenanceIssue> allIssues = new ConcurrentHashMap<>();
    private final Deque<String> technicians = new ArrayDeque<>(List.of(
            "Aziz Karimov", "Dilshod Tursunov", "Bobur Eshonov"
    ));

    public MaintenanceService(BrokerClient broker) {
        this.broker = broker;
    }

    public MaintenanceIssue report(String roomNumber, String description, Urgency urgency) {
        MaintenanceIssue issue = new MaintenanceIssue(roomNumber, description, urgency);
        queue.insert(issue);
        allIssues.put(issue.getId(), issue);

        String technician;
        synchronized (technicians) {
            technician = technicians.poll();
            if (technician != null) {
                technicians.offer(technician); // round-robin
            } else {
                technician = "Unassigned";
            }
        }
        issue.assignTo(technician);

        broker.publish(Topics.MAINTENANCE_REPORTED, Map.of(
                "issueId", issue.getId(),
                "roomNumber", roomNumber,
                "urgency", urgency.name(),
                "assignedTo", technician,
                "description", description
        ));
        return issue;
    }

    public MaintenanceIssue resolve(long id) {
        MaintenanceIssue issue = allIssues.get(id);
        if (issue == null || issue.isResolved()) return issue;
        issue.markResolved();
        queue.remove(id);

        broker.publish(Topics.MAINTENANCE_RESOLVED, Map.of(
                "issueId", id,
                "roomNumber", issue.getRoomNumber()
        ));
        return issue;
    }

    public List<MaintenanceIssue> queueSnapshot() {
        return queue.snapshot();
    }
}
