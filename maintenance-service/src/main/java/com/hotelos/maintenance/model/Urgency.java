package com.hotelos.maintenance.model;

/**
 * Ordered most-urgent first. The ordinal drives the PriorityQueue comparator,
 * so do not reorder these without thinking about that contract.
 */
public enum Urgency { CRITICAL, HIGH, NORMAL, LOW }
