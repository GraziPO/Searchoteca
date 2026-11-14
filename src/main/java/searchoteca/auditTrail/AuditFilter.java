package searchoteca.auditTrail;

import java.time.LocalDate;

/**
 * Filtros da consulta e da exportação da auditoria. Todos opcionais.
 *
 * @param username usuário que fez a ação (sem diferenciar maiúsculas)
 * @param action   nome da ação, ex.: UPDATE, LOGIN_FAILURE
 * @param from     a partir deste dia (inclusive)
 * @param to       até este dia (inclusive)
 */
public record AuditFilter(String username, String action, LocalDate from, LocalDate to) {

    public AuditFilter {
        username = blankToNull(username);
        action = action == null || action.isBlank() ? null : action.trim().toUpperCase();
    }

    /** Texto curto para registrar na própria auditoria quem exportou o quê. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (username != null) sb.append(" usuario=").append(username);
        if (action != null)   sb.append(" acao=").append(action);
        if (from != null)     sb.append(" de=").append(from);
        if (to != null)       sb.append(" ate=").append(to);
        return sb.isEmpty() ? " sem filtros" : sb.toString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
