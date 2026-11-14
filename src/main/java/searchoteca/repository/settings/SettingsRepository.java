package searchoteca.repository.settings;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tabela sys_settings: uma linha por configuração alterada pelo root. */
@Repository
public class SettingsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SettingsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Valor salvo de cada chave. Chave que nunca foi alterada não aparece (usa o padrão). */
    public Map<String, String> findAllValues() {
        Map<String, String> values = new HashMap<>();
        jdbc.query("SELECT setting_key, setting_value FROM sys_settings", Map.of(),
                rs -> { values.put(rs.getString("setting_key"), rs.getString("setting_value")); });
        return values;
    }

    /** Dados de quem alterou por último, para a tela do root. */
    public List<StoredSetting> findAll() {
        return jdbc.query("""
                SELECT s.setting_key, s.setting_value, s.updated_at, u.username AS updated_by
                FROM sys_settings s
                LEFT JOIN org_users u ON u.user_id = s.updated_by
                """, Map.of(), (rs, i) -> {
            Timestamp ts = rs.getTimestamp("updated_at");
            return new StoredSetting(rs.getString("setting_key"), rs.getString("setting_value"),
                    ts == null ? null : ts.toLocalDateTime(), rs.getString("updated_by"));
        });
    }

    /** Insere ou atualiza (upsert) uma configuração. */
    public void save(String key, String value, Long updatedBy) {
        jdbc.update("""
                INSERT INTO sys_settings (setting_key, setting_value, updated_at, updated_by)
                VALUES (:key, :value, now(), :by)
                ON CONFLICT (setting_key)
                DO UPDATE SET setting_value = EXCLUDED.setting_value,
                              updated_at    = EXCLUDED.updated_at,
                              updated_by    = EXCLUDED.updated_by
                """, new MapSqlParameterSource()
                .addValue("key", key).addValue("value", value).addValue("by", updatedBy));
    }

    public record StoredSetting(String key, String value, LocalDateTime updatedAt, String updatedBy) {}
}
