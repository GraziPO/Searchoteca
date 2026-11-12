package searchoteca.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.waitingLine.WaitlistEntry;
import searchoteca.model.waitingLine.WaitlistSummary;
import searchoteca.security.CustomUserDetails;
import searchoteca.service.WaitlistService;

import java.util.List;

@RestController
@RequestMapping("/api/fila")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final AuditLogService auditLogService;

    public WaitlistController(WaitlistService waitlistService, AuditLogService auditLogService) {
        this.waitlistService = waitlistService;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAuthority('waitlist:join')")
    @GetMapping("/minhas")
    public ResponseEntity<List<WaitlistEntry>> mine(Authentication auth) {
        return ResponseEntity.ok(waitlistService.mine(userId(auth)));
    }

    @PreAuthorize("hasAuthority('waitlist:join')")
    @GetMapping("/{isbn}")
    public ResponseEntity<WaitlistSummary> summary(@PathVariable String isbn, Authentication auth) {
        return ResponseEntity.ok(waitlistService.summary(userId(auth), isbn));
    }

    @PreAuthorize("hasAuthority('waitlist:join')")
    @PostMapping("/{isbn}")
    public ResponseEntity<WaitlistSummary> join(@PathVariable String isbn, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlistService.join(userId(auth), isbn));
    }

    @PreAuthorize("hasAuthority('waitlist:join')")
    @DeleteMapping("/{isbn}")
    public ResponseEntity<Void> leave(@PathVariable String isbn, Authentication auth) {
        waitlistService.leave(userId(auth), isbn);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('waitlist:view')")
    @GetMapping("/{isbn}/alunos")
    public ResponseEntity<List<WaitlistEntry>> queue(@PathVariable String isbn) {
        auditLogService.record(AuditAction.ACCESS, "fila de espera do ISBN " + isbn);
        return ResponseEntity.ok(waitlistService.queue(isbn));
    }

    private static Long userId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getUserId();
    }
}