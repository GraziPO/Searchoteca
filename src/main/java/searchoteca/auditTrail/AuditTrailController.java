package searchoteca.auditTrail;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditTrail")
public class AuditTrailController {

    private final AuditLogRepository logRepository;

    public AuditTrailController(AuditLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @PreAuthorize("hasAuthority('audit:view')")
    @GetMapping
    public ResponseEntity<List<AuditLogModel>> list(
            @RequestParam(name = "usuario", required = false) String usuario,
            @RequestParam(name = "limite", defaultValue = "100") int limite){
        int limit = Math.min(Math.max(limite,1), 500);
        List<AuditLogModel> result = (usuario == null || usuario.isBlank())
                ? logRepository.findRecent(limit)
                : logRepository.findRecentByUsername(usuario, limit);
        return ResponseEntity.ok().body(result);
    }
}