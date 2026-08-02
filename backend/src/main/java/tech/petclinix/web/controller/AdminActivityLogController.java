package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.petclinix.logic.domain.ActivityLogEntry;
import tech.petclinix.logic.service.ActivityLogService;

import java.util.List;

@RestController
@RequestMapping("/admin/activity-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityLogController {

    private final ActivityLogService activityLogService;

    public AdminActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityLogEntry>> get() {
        // ActivityLogEntry does not have any Http/Json specific annotations, so its ok to use it for serialzation directly.
        return ResponseEntity.ok(activityLogService.findRecent());
    }
}
