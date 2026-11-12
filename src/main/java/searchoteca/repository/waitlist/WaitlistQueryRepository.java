package searchoteca.repository.waitlist;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import searchoteca.model.waitingLine.WaitlistEntry;
import searchoteca.model.waitingLine.WaitlistNotice;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class WaitlistQueryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public WaitlistQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String RANKED = """
            WITH ranked AS (
                SELECT w.*,
                       ROW_NUMBER() OVER (PARTITION BY w.isbn
                                          ORDER BY CASE w.status WHEN 'NOTIFIED' THEN 0 ELSE 1 END,
                                                   w.created_at, w.waitlist_id) AS position
                FROM org_waitlist w
                WHERE w.status IN ('WAITING', 'NOTIFIED')
            )
            SELECT r.waitlist_id, r.isbn, b.title, b.author, r.status, r.position,
                   r.created_at, r.expires_at, c.copy_custom_code, c.local_code,
                   u.complete_name, s.ra
            FROM ranked r
            JOIN org_books b      ON b.isbn = r.isbn
            JOIN org_students s   ON s.student_id = r.student_id
            JOIN org_users u      ON u.user_id = s.user_id
            LEFT JOIN org_copies c ON c.copy_id = r.copy_id
            """;

    /** Filas em que o aluno está (sem nome e RA, que são dele mesmo). */
    public List<WaitlistEntry> findByStudent(Long studentId) {
        return jdbc.query(RANKED + " WHERE r.student_id = :studentId ORDER BY r.created_at",
                Map.of("studentId", studentId), (rs, i) -> entry(rs, false));
    }

    /** Fila completa de um título, para o operador. */
    public List<WaitlistEntry> findQueue(String isbn) {
        return jdbc.query(RANKED + " WHERE r.isbn = :isbn ORDER BY r.position",
                Map.of("isbn", isbn), (rs, i) -> entry(rs, true));
    }

    /** Contagem de exemplares do título: [disponíveis, em circulação]. */
    public int[] copyCounts(String isbn) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE status = 'AVAILABLE') AS available,
                       COUNT(*) FILTER (WHERE status IN ('AVAILABLE', 'LOANED', 'RESERVED')) AS circulating
                FROM org_copies WHERE isbn = :isbn
                """, Map.of("isbn", isbn),
                (rs, i) -> new int[]{rs.getInt("available"), rs.getInt("circulating")});
    }

    /** O aluno já está com algum exemplar deste título? */
    public boolean hasOpenLoanOfTitle(Long studentId, String isbn) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM org_loans l
                    JOIN org_loan_items li ON li.loan_id = l.loan_id AND li.returned_at IS NULL
                    JOIN org_copies c      ON c.copy_id = li.copy_id
                    WHERE l.student_id = :studentId AND l.status = 'OPEN' AND c.isbn = :isbn
                )
                """, Map.of("studentId", studentId, "isbn", isbn), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /** Dados para o e-mail de "seu exemplar está reservado". */
    public Optional<WaitlistNotice> findNotice(Long waitlistId) {
        List<WaitlistNotice> rows = jdbc.query("""
                SELECT u.email, u.complete_name, b.title, c.copy_custom_code, lo.local_name, w.expires_at
                FROM org_waitlist w
                JOIN org_students s  ON s.student_id = w.student_id
                JOIN org_users u     ON u.user_id = s.user_id
                JOIN org_books b     ON b.isbn = w.isbn
                JOIN org_copies c    ON c.copy_id = w.copy_id
                LEFT JOIN org_local lo ON lo.local_code = c.local_code
                WHERE w.waitlist_id = :id
                """, Map.of("id", waitlistId), (rs, i) -> new WaitlistNotice(
                rs.getString("email"),
                rs.getString("complete_name"),
                rs.getString("title"),
                rs.getString("copy_custom_code"),
                rs.getString("local_name"),
                toDateTime(rs.getTimestamp("expires_at"))));
        return rows.stream().findFirst();
    }

    private static WaitlistEntry entry(ResultSet rs, boolean withStudent) throws SQLException {
        return new WaitlistEntry(
                rs.getLong("waitlist_id"),
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("status"),
                rs.getInt("position"),
                toDateTime(rs.getTimestamp("created_at")),
                toDateTime(rs.getTimestamp("expires_at")),
                rs.getString("copy_custom_code"),
                rs.getString("local_code"),
                withStudent ? rs.getString("complete_name") : null,   // aluno não vê nome dos outros
                withStudent ? rs.getString("ra") : null);
    }

    private static LocalDateTime toDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}

