package searchoteca.service.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.settings.SettingDetail;
import searchoteca.DTO.settings.SettingsUpdateRequest;
import searchoteca.DTO.settings.SettingsView;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditDiff;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.exception.ResourceConflictException;
import searchoteca.model.settings.SettingKey;
import searchoteca.repository.settings.SettingsRepository;
import searchoteca.repository.settings.SettingsRepository.StoredSetting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurações da organização (definidas pelo root).
 *
 * O valor vem do banco (sys_settings). Se a chave nunca foi alterada, vale o padrão:
 *  - vendas, fila e inatividade: o valor do application.yaml (assim nada muda na primeira subida);
 *  - os demais: as constantes abaixo.
 * A leitura é feita no banco a cada uso: uma consulta pequena, e a mudança vale na hora, sem reiniciar.
 */
@Service
public class SettingsService {

    private final SettingsRepository repository;
    private final AuditLogService auditLogService;
    private final Map<SettingKey, Object> defaults = new EnumMap<>(SettingKey.class);

    public SettingsService(SettingsRepository repository,
                           AuditLogService auditLogService,
                           @Value("${sales.enabled:false}") boolean salesEnabled,
                           @Value("${waitlist.max-size:3}") int waitlistMaxSize,
                           @Value("${students.inactivity-days:90}") int inactivityDays) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        defaults.put(SettingKey.SALES_ENABLED, salesEnabled);
        defaults.put(SettingKey.LOAN_PERIOD_DAYS, 15);
        defaults.put(SettingKey.MAX_OPEN_LOANS, 5);
        defaults.put(SettingKey.BLOCK_OVERDUE, true);
        defaults.put(SettingKey.WAITLIST_MAX_SIZE, waitlistMaxSize);
        defaults.put(SettingKey.INACTIVITY_DAYS, inactivityDays);
    }

    // ===================================================================== leitura usada pelo sistema

    public boolean salesEnabled()           { return (boolean) current().get(SettingKey.SALES_ENABLED); }
    public int loanPeriodDays()             { return (int) current().get(SettingKey.LOAN_PERIOD_DAYS); }
    public int maxOpenLoansPerStudent()     { return (int) current().get(SettingKey.MAX_OPEN_LOANS); }
    public boolean blockOverdueLoans()      { return (boolean) current().get(SettingKey.BLOCK_OVERDUE); }
    public int waitlistMaxSize()            { return (int) current().get(SettingKey.WAITLIST_MAX_SIZE); }
    public int inactivityDays()             { return (int) current().get(SettingKey.INACTIVITY_DAYS); }

    public SettingsView view() {
        Map<SettingKey, Object> v = current();
        return new SettingsView(
                (boolean) v.get(SettingKey.SALES_ENABLED),
                (int) v.get(SettingKey.LOAN_PERIOD_DAYS),
                (int) v.get(SettingKey.MAX_OPEN_LOANS),
                (boolean) v.get(SettingKey.BLOCK_OVERDUE),
                (int) v.get(SettingKey.WAITLIST_MAX_SIZE),
                (int) v.get(SettingKey.INACTIVITY_DAYS));
    }

    /** Tela do root: cada configuração com valor, padrão, limites e última alteração. */
    public List<SettingDetail> details() {
        Map<SettingKey, Object> v = current();
        Map<String, StoredSetting> stored = new LinkedHashMap<>();
        for (StoredSetting s : repository.findAll()) {
            stored.put(s.key(), s);
        }
        List<SettingDetail> list = new ArrayList<>();
        for (SettingKey k : SettingKey.values()) {
            StoredSetting s = stored.get(k.key());
            boolean isInt = k.type() == SettingKey.Type.INTEGER;
            list.add(new SettingDetail(k.key(), k.label(), k.type().name(), v.get(k), defaults.get(k),
                    isInt ? k.min() : null, isInt ? k.max() : null,
                    s == null ? null : s.updatedAt(), s == null ? null : s.updatedBy()));
        }
        return list;
    }

    // ===================================================================== alteração (só root)

    /**
     * Valida tudo antes de gravar qualquer coisa: se um valor estiver fora do limite,
     * nada é salvo. Grava só o que mudou e registra o "antes -> depois" na auditoria.
     */
    @Transactional
    public SettingsView update(SettingsUpdateRequest request, Long rootUserId) {
        Map<SettingKey, Object> before = current();
        Map<SettingKey, Object> changes = new LinkedHashMap<>();

        collect(changes, SettingKey.SALES_ENABLED, request.salesEnabled());
        collect(changes, SettingKey.LOAN_PERIOD_DAYS, request.loanPeriodDays());
        collect(changes, SettingKey.MAX_OPEN_LOANS, request.maxOpenLoansPerStudent());
        collect(changes, SettingKey.BLOCK_OVERDUE, request.blockOverdueLoans());
        collect(changes, SettingKey.WAITLIST_MAX_SIZE, request.waitlistMaxSize());
        collect(changes, SettingKey.INACTIVITY_DAYS, request.inactivityDays());

        if (changes.isEmpty()) {
            throw new ResourceConflictException("Nenhuma configuração foi informada.");
        }

        AuditDiff diff = new AuditDiff();
        for (Map.Entry<SettingKey, Object> e : changes.entrySet()) {
            SettingKey key = e.getKey();
            diff.field(key.label(), before.get(key), e.getValue());
            if (!before.get(key).equals(e.getValue())) {
                repository.save(key.key(), String.valueOf(e.getValue()), rootUserId);
            }
        }
        if (diff.hasChanges()) {
            auditLogService.record(AuditAction.SETTINGS_UPDATED, diff.toString());
        }
        return view();
    }

    private void collect(Map<SettingKey, Object> changes, SettingKey key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Integer n && (n < key.min() || n > key.max())) {
            throw new ResourceConflictException(key.label() + ": informe um valor entre "
                    + key.min() + " e " + key.max() + ".");
        }
        changes.put(key, value);
    }

    // ===================================================================== auxiliares

    /** Padrões + o que estiver salvo no banco. Valor inválido no banco é ignorado (vale o padrão). */
    private Map<SettingKey, Object> current() {
        Map<SettingKey, Object> values = new EnumMap<>(defaults);
        for (Map.Entry<String, String> row : repository.findAllValues().entrySet()) {
            SettingKey key = SettingKey.fromKey(row.getKey());
            if (key == null || row.getValue() == null) continue;
            Object parsed = parse(key, row.getValue());
            if (parsed != null) values.put(key, parsed);
        }
        return values;
    }

    private static Object parse(SettingKey key, String raw) {
        String v = raw.trim();
        if (key.type() == SettingKey.Type.BOOLEAN) {
            if ("true".equalsIgnoreCase(v)) return true;
            if ("false".equalsIgnoreCase(v)) return false;
            return null;
        }
        try {
            int n = Integer.parseInt(v);
            return n >= key.min() && n <= key.max() ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
