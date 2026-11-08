package searchoteca.DTO.dashboard;

import java.time.LocalDate;

public record DelayedLoan(
        long loanId,
        String studentName,
        String ra,
        String copies,          // títulos ainda não devolvidos, separados por vírgula
        LocalDate dueDate,
        long delayedDays
) {
}
