package searchoteca.DTO.settings;

/**
 * Alteração feita pelo root. Todos os campos são opcionais:
 * o que vier null fica como está. Ex.: { "loanPeriodDays": 21 }
 */
public record SettingsUpdateRequest(
        Boolean salesEnabled,
        Integer loanPeriodDays,
        Integer maxOpenLoansPerStudent,
        Boolean blockOverdueLoans,
        Integer waitlistMaxSize,
        Integer inactivityDays) {
}
