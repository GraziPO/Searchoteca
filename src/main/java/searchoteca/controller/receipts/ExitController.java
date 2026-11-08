package searchoteca.controller.receipts;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import searchoteca.DTO.archive.SalesNExists.ExitRequest;
import searchoteca.DTO.archive.SalesNExists.ReceiptSnapshot;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.security.CustomUserDetails;
import searchoteca.service.ReceiptService;
import searchoteca.service.ExitService;

@RestController
@RequestMapping("/api/saidas")
public class ExitController {
    private final ExitService exitService;
    private final AuditLogService auditLogService;
    private final ReceiptService receiptService;

    public ExitController(ExitService exitService, AuditLogService auditLogService, ReceiptService receiptService) {
        this.exitService = exitService;
        this.auditLogService = auditLogService;
        this.receiptService = receiptService;
    }

    @PreAuthorize("hasAuthority('exits:create')")
    @PostMapping
    public ResponseEntity<?> register(@RequestBody ExitRequest request, Authentication auth) {
        CustomUserDetails operator = (CustomUserDetails) auth.getPrincipal();
        ReceiptSnapshot receipt = exitService.register(request, operator);
        auditLogService.record(AuditAction.CREATE, "saída " + receipt.exitType() + " — comprovante " + receipt.code());
        return ResponseEntity.ok(receipt);
    }

    @PreAuthorize("hasAuthority('receipts:view')")
    @GetMapping("/api/comprovantes/{code}")
    public ResponseEntity<String> receipt(@PathVariable String code) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(receiptService.findByCode(code));
    }
}
