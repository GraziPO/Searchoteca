package searchoteca.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.dashboard.DashboardResponse;
import searchoteca.auditTrail.AuditLogRepository;
import searchoteca.repository.DashboardRepository;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;
    private static final int DELAYED_LIMIT = 10;
    private static final int ACTIVITY_LIMIT = 8;

    private final DashboardRepository dashboardRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(DashboardRepository dashboardRepository,
                            AuditLogRepository auditLogRepository) {
        this.dashboardRepository = dashboardRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * readOnly: nada é gravado, e todas as consultas enxergam o mesmo momento do banco,
     * então os números dos cards e dos gráficos batem entre si.
     */
    @Transactional(readOnly = true)
    public DashboardResponse load(int days) {
        int period = Math.clamp(days, MIN_DAYS, MAX_DAYS);

        return new DashboardResponse(
                period,
                LocalDateTime.now(),
                dashboardRepository.kpis(period),
                dashboardRepository.diary(period),
                dashboardRepository.copiesByStatus(),
                dashboardRepository.departmentOccupation(),
                dashboardRepository.delayed(DELAYED_LIMIT),
                dashboardRepository.security(),
                auditLogRepository.findRecent(ACTIVITY_LIMIT));
    }
}
