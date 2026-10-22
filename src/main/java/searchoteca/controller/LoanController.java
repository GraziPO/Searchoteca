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
    @PreAuthorize("hasAuthority('loans:view_own')")
    @GetMapping("/historico")
    public ResponseEntity<?> findOwn(Authentication authentication) {
        CustomUserDetails student = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(loanService.findAllByStudent(student.getUserId()));
    }

    @PreAuthorize("hasAuthority('loans:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody LoanModel loanModel, String ra) {
        return ResponseEntity.ok(loanService.create(loanModel, ra));
    }

    @PreAuthorize("hasAuthority('loans:cancel')")
    @PostMapping("{loan_id}/cancelar")
    public ResponseEntity<?> cancel(@PathVariable Long loan_id, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok( loanService.cancelLoan(loan_id,user.getUserId()));
    }
}
