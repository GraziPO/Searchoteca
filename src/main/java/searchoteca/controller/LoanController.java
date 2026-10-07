package searchoteca.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.model.LoanModel;
import searchoteca.repository.LoanItemRepository;
import searchoteca.repository.LoanRepository;
import searchoteca.service.LoanService;
import searchoteca.service.StudentLoanService;

import java.util.List;

@RestController
@RequestMapping("api/emprestimos")
public class LoanController {
    private final LoanService loanService;
    private final StudentLoanService studentLoanService;

    public LoanController(LoanService loanService, StudentLoanService studentLoanService) {
        this.loanService = loanService;
        this.studentLoanService = studentLoanService;
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(loanService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable String id) {
        return ResponseEntity.ok(loanService.findById(id));
    }
    //endpoint exclusivo para os alunos
    @GetMapping("/alunos/{studentId}")
    public ResponseEntity<?> findByStudentId(@PathVariable String id) {
        return ResponseEntity.ok(loanService.findByUsername(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LoanModel loanModel) {
        return ResponseEntity.ok(loanService.create(loanModel));
    }

}
