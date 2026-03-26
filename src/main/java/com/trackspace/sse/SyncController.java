package com.trackspace.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for managing sync heartbeats.
 * Frontend calls heartbeat while on GitHub/Jira pages to keep sync active.
 */
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final ScheduledSyncService scheduledSyncService;

    @PostMapping("/heartbeat/{projectId}")
    public ResponseEntity<Void> heartbeat(@PathVariable("projectId") Integer projectId) {
        scheduledSyncService.heartbeat(projectId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deactivate/{projectId}")
    public ResponseEntity<Void> deactivate(@PathVariable("projectId") Integer projectId) {
        scheduledSyncService.deactivate(projectId);
        return ResponseEntity.ok().build();
    }
}
