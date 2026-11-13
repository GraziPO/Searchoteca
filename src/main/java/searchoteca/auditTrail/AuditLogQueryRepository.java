package searchoteca.auditTrail;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * Consulta da trilha de auditoria com filtros opcionais.
 * Cada filtro só entra no WHERE se foi informado; os valores vão sempre como parâmetro (nada de concatenar texto do usuário no SQL).
 */
@Repository
public class AuditLogQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AuditLogQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AuditLogModel> search(AuditFilter filter, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, time_stamp, username, action, details
                FROM auditTrail_logs
                WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);

        if (filter.username() != null) {
            sql.append(" AND lower(username) = lower(:username)");
            params.addValue("username", filter.username());
        }
        if (filter.action() != null) {
            sql.append(" AND action = :action");
            params.addValue("action", filter.action());
        }
        if (filter.from() != null) {
            sql.append(" AND time_stamp >= :from");
            params.addValue("from", Timestamp.valueOf(filter.from().atStartOfDay()));
        }
        if (filter.to() != null) {
            // "até o dia 10" inclui o dia 10 inteiro: menor que o começo do dia 11
            sql.append(" AND time_stamp < :to");
            params.addValue("to", Timestamp.valueOf(filter.to().plusDays(1).atStartOfDay()));
        }
        sql.append(" ORDER BY time_stamp DESC, id DESC LIMIT :limit");

        return jdbc.query(sql.toString(), params, (rs, i) -> {
            AuditLogModel log = new AuditLogModel();
            log.setId(rs.getLong("id"));
            Timestamp ts = rs.getTimestamp("time_stamp");
            log.setTime_stamp(ts == null ? null : ts.toLocalDateTime());
            log.setUsername(rs.getString("username"));
            log.setAction(rs.getString("action"));
            log.setDetails(rs.getString("details"));
            return log;
        });
    }
}
