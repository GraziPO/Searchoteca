package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.DTO.CopyLabel;
import searchoteca.DTO.CopyRequest;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.service.CopyService;

import java.util.List;

@RestController
@RequestMapping("api/exemplares")
public class CopyController {
    private final CopyService copyService;
    private final AuditLogService auditLogService;

    public CopyController(CopyService copyService, AuditLogService auditLogService) {
        this.copyService = copyService;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAuthority('copies:create')")
    @PostMapping()
    public ResponseEntity<?> createCopy(@RequestBody CopyRequest request){
        List<CopyLabel> labels = copyService.create(request);
        auditLogService.record(AuditAction.CREATE,
                request.quantity() + " exemplar(es) do ISBN " + request.isbn() + " em " + request.localCode());
        return ResponseEntity.ok(labels);
    }

    //rotas específicas pra impressão de etiquetas
    @PreAuthorize("hasAuthority('copies:view')")
    @GetMapping("/etiquetas")
    public ResponseEntity<?> printLabels(@RequestParam List<Long> labels){
        auditLogService.record(AuditAction.ACCESS,"reipressão de etiquetas: " + labels);
        return ResponseEntity.ok(copyService.findLabels(labels));
    }
}
