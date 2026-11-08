package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchoteca.DTO.dashboard.DashboardResponse;
import searchoteca.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** GET /api/dashboard?dias=30 — período entre 1 e 365 dias (fora disso é ajustado no service). */
    @PreAuthorize("hasAuthority('dashboard:view')")
    @GetMapping
    public ResponseEntity<DashboardResponse> dashboard(
            @RequestParam(name = "dias", defaultValue = "30") int dias) {
        return ResponseEntity.ok(dashboardService.load(dias));
    }
}
