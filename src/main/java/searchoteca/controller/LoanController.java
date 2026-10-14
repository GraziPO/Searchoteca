package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import searchoteca.model.LoanModel;
import searchoteca.security.CustomUserDetails;
import searchoteca.service.LoanService;

@RestController
@RequestMapping("api/emprestimos")
public class LoanController {
    private final LoanService loanService;


    public LoanController(LoanService loanService) {
        this.loanService = loanService;
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
    @PreAuthorize("hasAuthority('students:view')")
    @GetMapping("/alunos/{studentId}")
    public ResponseEntity<?> findByStudentId(@PathVariable String id) {
        return ResponseEntity.ok(loanService.findByUsername(id));
    }

    @PreAuthorize("hasAuthority('loans:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody LoanModel loanModel) {
        return ResponseEntity.ok(loanService.create(loanModel));
    }

    @PreAuthorize("hasAuthority('loans:cancel')")
    @PostMapping("{loan_id}/cancelar")
    public ResponseEntity<?> cancel(@PathVariable Long loanId, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        LoanModel loan = loanService.cancelLoan(loanId, user.getUserId());
        return ResponseEntity.ok(loan);
    }
}
