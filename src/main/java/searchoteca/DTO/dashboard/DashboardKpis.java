package searchoteca.DTO.dashboard;

import java.math.BigDecimal;

public record DashboardKpis(
        long openLoans,
        long delayedLoans,
        long activeStudents,
        long inactiveStudents,      // ativos sem atividade há mais de 80 dias (depende da RF17 atualizar last_activity)
        long availableCopies,
        long totalCopies,
        long pendingFirstAccess,
        long loansInPeriod,
        long returnsInPeriod,
        long exitsInPeriod,         // vendas, doações e baixas (comprovantes que não são de empréstimo)
        BigDecimal salesThisMonth,
        long receiptsThisMonth) {

    /** Total de movimentações no período: empréstimos + devoluções + saídas. */
    public long movementsInPeriod() {
        return loansInPeriod + returnsInPeriod + exitsInPeriod;
    }
}
