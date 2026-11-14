package searchoteca.auditTrail;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchoteca.exception.ResourceConflictException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/auditTrail")
public class AuditTrailController {

    /** Teto da consulta e da exportação (RF21: até 500 registros por arquivo). */
    private static final int MAX_ROWS = 500;

    private final AuditLogQueryRepository queryRepository;
    private final AuditLogService auditLogService;

    public AuditTrailController(AuditLogQueryRepository queryRepository, AuditLogService auditLogService) {
        this.queryRepository = queryRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Consulta na tela. Exemplos:
     *   GET /api/auditTrail?limite=50
     *   GET /api/auditTrail?usuario=maria&acao=UPDATE&de=2026-11-01&ate=2026-11-13
     */
    @PreAuthorize("hasAuthority('audit:view')")
    @GetMapping
    public ResponseEntity<List<AuditLogModel>> list(
            @RequestParam(name = "usuario", required = false) String usuario,
            @RequestParam(name = "acao", required = false) String acao,
            @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(name = "limite", defaultValue = "100") int limite) {
        AuditFilter filter = filter(usuario, acao, de, ate);
        return ResponseEntity.ok(queryRepository.search(filter, clamp(limite)));
    }

    /**
     * Exportação em CSV (RF21), com os mesmos filtros da consulta. Devolve os registros
     * mais recentes, no máximo 500. A própria exportação fica registrada na auditoria.
     *   GET /api/auditTrail/exportar?de=2026-11-01&ate=2026-11-13
     */
    @PreAuthorize("hasAuthority('audit:view')")
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> export(
            @RequestParam(name = "usuario", required = false) String usuario,
            @RequestParam(name = "acao", required = false) String acao,
            @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(name = "limite", defaultValue = "500") int limite) {
        AuditFilter filter = filter(usuario, acao, de, ate);
        List<AuditLogModel> logs = queryRepository.search(filter, clamp(limite));
        byte[] csv = AuditCsvWriter.write(logs);

        auditLogService.record(AuditAction.AUDIT_EXPORTED, logs.size() + " registros;" + filter.describe());

        String fileName = "auditoria_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header("X-Total-Rows", String.valueOf(logs.size()))
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    private static AuditFilter filter(String usuario, String acao, LocalDate de, LocalDate ate) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new ResourceConflictException("A data inicial não pode ser depois da data final.");
        }
        return new AuditFilter(usuario, acao, de, ate);
    }

    private static int clamp(int limite) {
        return Math.min(Math.max(limite, 1), MAX_ROWS);
    }
}
