package com.hotelos.maintenance.service;

import com.hotelos.maintenance.model.MaintenanceIssue;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Implements the Maintenance Priority Queue algorithm (Task 1.2).
 *
 * Ordering:
 *   1. Urgency ordinal — CRITICAL=0 < HIGH=1 < NORMAL=2 < LOW=3 (smaller first)
 *   2. Tie-break by submittedAt ASC — the issue submitted first goes first
 *      among issues of equal urgency (assignment guidance: "first submitted
 *      takes priority").
 *
 * Implemented as java.util.PriorityQueue (binary heap). Insert and poll are
 * O(log n), which is the right cost profile for a queue that receives reports
 * continuously and is drained by technicians.
 */
public class MaintenancePriorityQueue {

    private static final Comparator<MaintenanceIssue> ORDER = Comparator
            .comparingInt((MaintenanceIssue i) -> i.getUrgency().ordinal())
            .thenComparing(MaintenanceIssue::getSubmittedAt);

    private final PriorityQueue<MaintenanceIssue> heap = new PriorityQueue<>(ORDER);

    public synchronized void insert(MaintenanceIssue issue) {
        heap.offer(issue);
    }

    public synchronized MaintenanceIssue pollNext() {
        return heap.poll();
    }

    public synchronized java.util.List<MaintenanceIssue> snapshot() {
        return heap.stream().sorted(ORDER).toList();
    }

    public synchronized boolean remove(long id) {
        return heap.removeIf(i -> i.getId() == id);
    }

    public synchronized int size() {
        return heap.size();
    }
}
