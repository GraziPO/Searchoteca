package searchoteca.DTO.dashboard;

import java.time.LocalDate;

public record DelayedLoan(
        long loanId,
        String aluno,
        String copy,
        LocalDate dueDate,
        long delayedDays
) {
}
