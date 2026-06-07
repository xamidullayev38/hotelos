package com.hotelos.maintenance.controller;

import com.hotelos.maintenance.model.MaintenanceIssue;
import com.hotelos.maintenance.model.Urgency;
import com.hotelos.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Validated
public class MaintenanceController {

    private final MaintenanceService service;

    public MaintenanceController(MaintenanceService service) {
        this.service = service;
    }

    public static class ReportRequest {
        @Pattern(regexp = "^[1-2][0-9]{2}$", message = "roomNumber must be 100-299")
        public String roomNumber;

        @NotBlank
        public String description;

        @NotNull
        public Urgency urgency;
    }

    @GetMapping("/queue")
    public List<MaintenanceIssue> queue() {
        return service.queueSnapshot();
    }

    @PostMapping("/report")
    public MaintenanceIssue report(@Valid @RequestBody ReportRequest req) {
        return service.report(req.roomNumber, req.description, req.urgency);
    }

    @PostMapping("/resolve/{id}")
    public ResponseEntity<?> resolve(@PathVariable long id) {
        MaintenanceIssue issue = service.resolve(id);
        if (issue == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "ISSUE_NOT_FOUND", "message", "No issue with id " + id
            ));
        }
        return ResponseEntity.ok(issue);
    }
}
