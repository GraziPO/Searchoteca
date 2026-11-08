package searchoteca.DTO.dashboard;

/** Contagens da trilha de auditoria nas últimas 24 horas. */
public record SecuritySummary(
        long loginSuccess,
        long loginFailure,
        long mfaFailure,
        long accessDenied,
        long passwordResets
) {
}
