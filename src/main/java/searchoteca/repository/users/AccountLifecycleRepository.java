package searchoteca.repository.users;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import searchoteca.DTO.users.account.AccountRequestView;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SQL da desativação, da reativação e da anonimização de alunos,
 * e dos pedidos em sys_account_requests.
 */
@Repository
public class AccountLifecycleRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AccountLifecycleRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ===================================================================== inatividade

    /**
     * Alunos ativos sem empréstimo nem devolução desde a data de corte.
     * Ficam de fora quem ainda está com livro emprestado (precisa devolver antes)
     * e quem está na fila de espera (está esperando um livro, não sumiu).
     */
    public List<Long> findInactiveStudentUserIds(int days) {
        return jdbc.queryForList("""
                SELECT u.user_id
                FROM org_students s
                JOIN org_users u ON u.user_id = s.user_id
                WHERE u.role_code = 'students'
                  AND u.status
                  AND s.anonymized_at IS NULL
                  AND COALESCE(s.last_activity, DATE '1900-01-01') < CURRENT_DATE - CAST(:days AS int)
                  AND NOT EXISTS (SELECT 1 FROM org_loans l
                                  WHERE l.student_id = s.student_id AND l.status = 'OPEN')
                  AND NOT EXISTS (SELECT 1 FROM org_waitlist w
                                  WHERE w.student_id = s.student_id AND w.status IN ('WAITING', 'NOTIFIED'))
                ORDER BY u.user_id
                """, Map.of("days", days), Long.class);
    }

    // ===================================================================== estado da conta

    public void markDeactivated(Long userId, String reason) {
        jdbc.update("UPDATE org_users SET status = FALSE WHERE user_id = :id", Map.of("id", userId));
        jdbc.update("""
                UPDATE org_students SET deactivated_at = now(), deactivation_reason = :reason
                WHERE user_id = :id
                """, Map.of("id", userId, "reason", reason));
    }

    /**
     * Reativa e "zera o relógio" da inatividade (last_activity = hoje); sem isso,
     * o job da madrugada desativaria o aluno de novo na mesma noite.
     * Não faz nada com conta anonimizada.
     */
    public int markReactivated(Long userId) {
        int rows = jdbc.update("""
                UPDATE org_students
                SET deactivated_at = NULL, deactivation_reason = NULL, last_activity = CURRENT_DATE
                WHERE user_id = :id AND anonymized_at IS NULL
                """, Map.of("id", userId));
        if (rows > 0) {
            jdbc.update("UPDATE org_users SET status = TRUE WHERE user_id = :id", Map.of("id", userId));
        }
        return rows;
    }

    /** Motivo da desativação do aluno (INACTIVITY, ADMIN, REQUEST) ou null se não houver. */
    public String findDeactivationReason(Long userId) {
        List<String> rows = jdbc.queryForList(
                "SELECT deactivation_reason FROM org_students WHERE user_id = :id",
                Map.of("id", userId), String.class);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public boolean isAnonymized(Long userId) {
        Boolean anon = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM org_students WHERE user_id = :id AND anonymized_at IS NOT NULL)
                """, Map.of("id", userId), Boolean.class);
        return Boolean.TRUE.equals(anon);
    }

    public boolean hasOpenLoans(Long studentId) {
        Boolean open = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM org_loans WHERE student_id = :sid AND status = 'OPEN')
                """, Map.of("sid", studentId), Boolean.class);
        return Boolean.TRUE.equals(open);
    }

    // ===================================================================== anonimização

    /**
     * Troca todos os dados pessoais do aluno por valores sem significado.
     * Empréstimos, comprovantes e auditoria continuam existindo (estatística e histórico),
     * mas não identificam mais a pessoa.
     *
     * @param old           dados atuais, para achar onde eles aparecem
     * @param passwordHash  hash de uma senha aleatória: ninguém mais entra com esta conta
     */
    public void anonymize(PersonalData old, String passwordHash) {
        String anonUser = "anon_" + old.userId();
        String anonEmail = "anon_" + old.userId() + "@anonimizado.invalid";
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", old.userId())
                .addValue("sid", old.studentId())
                .addValue("anonUser", anonUser)
                .addValue("anonEmail", anonEmail)
                .addValue("hash", passwordHash)
                .addValue("oldUser", old.username())
                .addValue("oldRa", old.ra());

        // 1) cadastro do usuário
        jdbc.update("""
                UPDATE org_users
                SET username = :anonUser, complete_name = 'Aluno anonimizado', email = :anonEmail,
                    custom_id = NULL, password = :hash, status = FALSE, must_change_password = FALSE
                WHERE user_id = :id
                """, p);

        // 2) dados de aluno
        jdbc.update("""
                UPDATE org_students
                SET ra = NULL, phone = NULL, anonymized_at = now(),
                    deactivated_at = COALESCE(deactivated_at, now()), deactivation_reason = 'REQUEST'
                WHERE student_id = :sid
                """, p);

        // 3) códigos de MFA e de redefinição de senha, contador de login
        jdbc.update("DELETE FROM sys_mfa WHERE user_id = :id", p);
        jdbc.update("DELETE FROM sys_password_reset WHERE user_id = :id", p);
        jdbc.update("DELETE FROM sys_login_attempts WHERE username = :oldUser", p);

        // 4) texto livre que o próprio aluno escreveu nos pedidos
        jdbc.update("UPDATE sys_account_requests SET reason = NULL WHERE user_id = :id", p);

        // 5) comprovantes: o snapshot guarda nome e RA do aluno
        if (old.ra() != null) {
            jdbc.update("""
                    UPDATE org_receipts
                    SET snapshot = jsonb_set(snapshot, '{student}',
                                             jsonb_build_object('ra', NULL, 'name', 'Aluno anonimizado'))
                    WHERE snapshot->'student'->>'ra' = :oldRa
                    """, p);
        }

        // 6) trilha de auditoria: o registro continua, mas sem quem era
        jdbc.update("UPDATE auditTrail_logs SET username = :anonUser WHERE username = :oldUser", p);
        for (String value : old.searchableValues()) {
            jdbc.update("""
                    UPDATE auditTrail_logs SET details = replace(details, :value, '[anonimizado]')
                    WHERE details LIKE '%' || :value || '%'
                    """, Map.of("value", value));
        }
    }

    public Optional<PersonalData> findPersonalData(Long userId) {
        List<PersonalData> rows = jdbc.query("""
                SELECT u.user_id, s.student_id, u.username, u.complete_name, u.email, s.ra
                FROM org_users u JOIN org_students s ON s.user_id = u.user_id
                WHERE u.user_id = :id
                """, Map.of("id", userId), (rs, i) -> new PersonalData(
                rs.getLong("user_id"), rs.getLong("student_id"), rs.getString("username"),
                rs.getString("complete_name"), rs.getString("email"), rs.getString("ra")));
        return rows.stream().findFirst();
    }

    /** O que identifica o aluno e precisa sumir dos textos. */
    public record PersonalData(Long userId, Long studentId, String username,
                               String completeName, String email, String ra) {
        /** Valores curtos demais (ex.: um username de 3 letras) estragariam outros textos no replace. */
        List<String> searchableValues() {
            return java.util.stream.Stream.of(ra, email, completeName, username)
                    .filter(v -> v != null && v.length() >= 5)
                    .toList();
        }
    }

    // ===================================================================== pedidos

    public long insertRequest(Long userId, String type, String reason) {
        Long id = jdbc.queryForObject("""
                INSERT INTO sys_account_requests (user_id, type, status, reason, created_at)
                VALUES (:id, :type, 'PENDING', :reason, now())
                RETURNING request_id
                """, new MapSqlParameterSource()
                .addValue("id", userId).addValue("type", type).addValue("reason", reason), Long.class);
        return id == null ? 0 : id;
    }

    public boolean hasPendingRequest(Long userId) {
        Boolean pending = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM sys_account_requests WHERE user_id = :id AND status = 'PENDING')
                """, Map.of("id", userId), Boolean.class);
        return Boolean.TRUE.equals(pending);
    }

    public int cancelPending(Long userId, String type) {
        return jdbc.update("""
                UPDATE sys_account_requests SET status = 'CANCELLED', decided_at = now()
                WHERE user_id = :id AND type = :type AND status = 'PENDING'
                """, Map.of("id", userId, "type", type));
    }

    /** Trava o pedido até o fim da transação: dois cliques em "aprovar" não aprovam duas vezes. */
    public Optional<AccountRequestView> findForUpdate(Long requestId) {
        return query("WHERE r.request_id = :rid FOR UPDATE OF r", Map.of("rid", requestId)).stream().findFirst();
    }

    public void decide(Long requestId, String status, Long decidedBy, String note) {
        jdbc.update("""
                UPDATE sys_account_requests
                SET status = :status, decided_at = now(), decided_by = :by, decision_note = :note
                WHERE request_id = :rid
                """, new MapSqlParameterSource()
                .addValue("rid", requestId).addValue("status", status)
                .addValue("by", decidedBy).addValue("note", note));
    }

    /**
     * Lista os pedidos. status null = os 200 mais recentes; type null = todos os tipos
     * (o admin_users só enxerga os de REACTIVATE).
     */
    public List<AccountRequestView> findByStatus(String status, String type) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("status", status).addValue("type", type);
        String where = " WHERE (CAST(:status AS VARCHAR) IS NULL OR r.status = :status)"
                + " AND (CAST(:type AS VARCHAR) IS NULL OR r.type = :type)";
        return status == null
                ? query(where + " ORDER BY r.created_at DESC LIMIT 200", p)
                : query(where + " ORDER BY r.created_at", p);
    }

    public List<AccountRequestView> findByUser(Long userId) {
        return query("WHERE r.user_id = :id ORDER BY r.created_at DESC", Map.of("id", userId));
    }

    private List<AccountRequestView> query(String tail, Map<String, ?> params) {
        return query(tail, new MapSqlParameterSource(params));
    }

    private List<AccountRequestView> query(String tail, MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT r.request_id, r.user_id, u.username, u.complete_name, s.ra, r.type, r.status,
                       r.reason, r.created_at, r.decided_at, d.username AS decided_by, r.decision_note,
                       s.deactivation_reason
                FROM sys_account_requests r
                JOIN org_users u       ON u.user_id = r.user_id
                LEFT JOIN org_students s ON s.user_id = r.user_id
                LEFT JOIN org_users d  ON d.user_id = r.decided_by
                """ + tail, params, (rs, i) -> new AccountRequestView(
                rs.getLong("request_id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("complete_name"),
                rs.getString("ra"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getString("reason"),
                toDateTime(rs.getTimestamp("created_at")),
                toDateTime(rs.getTimestamp("decided_at")),
                rs.getString("decided_by"),
                rs.getString("decision_note"),
                rs.getString("deactivation_reason")));
    }

    private static LocalDateTime toDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
