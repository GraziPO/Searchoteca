package searchoteca.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LoginAttemptRepository {

    private final NamedParameterJdbcTemplate jdbc;
    public LoginAttemptRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    public int registerFailure(String username, int windowMinutes) {
        Integer attempts = jdbc.queryForObject("""
                INSERT INTO sys_login_attempts (username, failed_attempts, last_failure_at, locked_until)
                VALUES (:username, 1, now(), NULL)
                ON CONFLICT (username) DO UPDATE SET
                    failed_attempts = CASE
                        WHEN sys_login_attempts.last_failure_at < now() - make_interval(mins => :window)
                        THEN 1
                        ELSE sys_login_attempts.failed_attempts + 1
                    END,
                    last_failure_at = now()
                RETURNING failed_attempts
                """, Map.of("username", username, "window", windowMinutes), Integer.class);
        return attempts == null ? 1 : attempts;
    }

    /** Segundos que faltam de bloqueio, se o usuário estiver bloqueado agora. */
    public Optional<Long> secondsLocked(String username) {
        List<Long> rows = jdbc.query("""
                SELECT CEIL(EXTRACT(EPOCH FROM (locked_until - now())))::bigint AS seconds
                FROM sys_login_attempts
                WHERE username = :username AND locked_until > now()
                """, Map.of("username", username), (rs, i) -> rs.getLong("seconds"));
        return rows.stream().findFirst();
    }

    /** Bloqueia por lockMinutes e zera o contador. */
    public void lock(String username, int lockMinutes) {
        jdbc.update("""
                UPDATE sys_login_attempts
                SET locked_until = now() + make_interval(mins => :minutes), failed_attempts = 0
                WHERE username = :username
                """, Map.of("username", username, "minutes", lockMinutes));
    }

    /** Login certo: apaga o histórico de falhas. */
    public void clear(String username) {
        jdbc.update("DELETE FROM sys_login_attempts WHERE username = :username",
                Map.of("username", username));
    }
}
