package searchoteca.DTO.dashboard;

import searchoteca.auditTrail.AuditLogModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Tudo o que a tela do dashboard precisa, numa única resposta. */
public record DashboardResponse(
        int periodDays,
        LocalDate generatedAt,
        DashboardKpis kpis,
        List<Diary> diary,
        List<CountByStatus> copiesByStatus,
        List<DepartmentOccupation> departmentOccupation,
        List<DelayedLoan> delayedLoans,
        SecuritySummary security,
        List<AuditLogModel> recentActivity
) {
}
