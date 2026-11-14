package searchoteca.DTO.settings;

import java.time.LocalDateTime;

/**
 * Uma linha da tela de configurações do root: valor atual, padrão, limites
 * e quem mudou por último (null se nunca foi alterada).
 */
public record SettingDetail(
        String key,
        String label,
        String type,
        Object value,
        Object defaultValue,
        Integer min,
        Integer max,
        LocalDateTime updatedAt,
        String updatedBy) {
}
