package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import searchoteca.DTO.archive.LoanRequest;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.LoanModel;
import searchoteca.security.CustomUserDetails;
import searchoteca.service.LoanService;

@RestController
@RequestMapping("api/emprestimos")
public class LoanController {
    private final LoanService loanService;
    private final AuditLogService auditLogService;


    public LoanController(LoanService loanService, AuditLogService auditLogService) {
        this.loanService = loanService;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAuthority('loans:view')")
    @GetMapping
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(loanService.findAll());
    }

    @PreAuthorize("hasAuthority('loans:view')")
    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.findById(id));
    }


    //endpoint exclusivo para os alunos
    @PreAuthorize("hasAuthority('loans:view_own')")
    @GetMapping("/historico")
    public ResponseEntity<?> findOwn(Authentication authentication) {
        CustomUserDetails student = (CustomUserDetails) authentication.getPrincipal();
        auditLogService.record(AuditAction.ACCESS,"Acesso ao histórico do usuario");
        return ResponseEntity.ok(loanService.findAllByStudent(student.getUserId()));
    }

    @PreAuthorize("hasAuthority('loans:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody LoanRequest request, Authentication auth) {
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        auditLogService.save(user.getUsername(), AuditAction.CREATE,"usuário do tipo "+ user.getRoleCode()+ " criado");
        return ResponseEntity.ok(loanService.create(request, user.getUserId()));
    }

    @PreAuthorize("hasAuthority('loans:cancel')")
    @PostMapping("{loan_id}/cancelar")
    public ResponseEntity<?> cancel(@PathVariable Long loan_id, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        LoanModel loan = loanService.cancelLoan(loan_id,user.getUserId());
        auditLogService.save(user.getUsername(), AuditAction.UPDATE,"registro de empréstimo" + "cancelado por "+ user.getUsername());
        return ResponseEntity.ok(loan);
    }

    @PreAuthorize("hasAuthority('loans:return')")
    @PostMapping("/devolucao/{copyId}")
    public ResponseEntity<?> returnItem(@PathVariable Long copyId, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        LoanModel loan = loanService.returnItem(copyId,user.getUserId());
        auditLogService.save(user.getUsername(), AuditAction.UPDATE,"registro de empréstimo" + "devovido ao usuario "+ user.getUsername());
        return ResponseEntity.ok(loan);
    }
}
