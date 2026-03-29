package tech.petclinix.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.petclinix.logic.service.StatsService;
import tech.petclinix.web.dto.StatsResponse;

@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ResponseEntity<StatsResponse> get() {
        return ResponseEntity.ok(statsService.getStats());
    }
}
