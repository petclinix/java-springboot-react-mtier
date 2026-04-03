package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.petclinix.logic.service.StatsService;
import tech.petclinix.logic.domain.StatsData;

@RestController
@RequestMapping("/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ResponseEntity<StatsData> get() {
        // StatsData does not have any Http/Json specific annotations, so its ok to use it for serialzation directly.
        // If we needed to do any transformation, we could create a separate DTO and mapper for it.
        return ResponseEntity.ok(statsService.getStats());
    }
}
