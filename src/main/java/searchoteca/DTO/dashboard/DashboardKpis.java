package searchoteca.DTO.dashboard;

public record DashboardKpis(
        long openLoans,
        long delayedLoans,
        long activeStudents,
        long availableCopy,
        long totalCopies,
        long pendingFirstAccess) {}
