package searchoteca.DTO.settings;

/**
 * Valores em vigor. Qualquer usuário logado pode ler: o frontend usa para
 * esconder Vendas, mostrar o prazo de devolução, o limite de livros etc.
 */
public record SettingsView(
        boolean salesEnabled,
        int loanPeriodDays,
        int maxOpenLoansPerStudent,
        boolean blockOverdueLoans,
        int waitlistMaxSize,
        int inactivityDays) {
}
