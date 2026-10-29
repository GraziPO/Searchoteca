package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.BookModel;
import searchoteca.repository.CopyRepository;
import searchoteca.service.BookService;
import searchoteca.service.CopyService;

import java.util.List;

@RestController
@RequestMapping("/api/livro")
public class BookController {

    private final BookService bookService;
    private final CopyService copyService;
    private final AuditLogService auditLogService;

    public BookController(BookService bookService, CopyService copyService, AuditLogService auditLogService) {
        this.bookService=bookService;
        this.copyService=copyService;
        this.auditLogService=auditLogService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(){
        auditLogService.record(AuditAction.ACCESS,"All books");
        return ResponseEntity.ok(bookService.findAll());
    }

    @PreAuthorize("hasAuthority('org_books:view')")
    @GetMapping("/{isbn}")
    public ResponseEntity<?> getByIsbn(@PathVariable String isbn){
        BookModel book = bookService.findByIsbn(isbn);
        if (book == null){
            return ResponseEntity.notFound().build();
        }
        auditLogService.record(AuditAction.ACCESS,"org_books" + isbn);
        return ResponseEntity.ok(book);
    }

    @PreAuthorize("hasAuthority('org_books:create')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody BookModel Book){
        return ResponseEntity.ok(bookService.create(Book));
    }

    @PreAuthorize("hasAuthority('org_books:edit')")
    @PutMapping("/{isbn}")
    public ResponseEntity<?> update(@PathVariable String isbn, @RequestBody BookModel bookInfo){
        auditLogService.record(AuditAction.UPDATE,"org_books" + isbn + " " + bookInfo);
        return ResponseEntity.ok(bookService.update(isbn, bookInfo));
    }

    @PreAuthorize("hasAuthority('org_books:delete')")
    @DeleteMapping("/{isbn}")
    public ResponseEntity<?> delete(@PathVariable String isbn) {
        bookService.delete(isbn);
        auditLogService.record(AuditAction.DELETE,"org_books" + isbn);
        return ResponseEntity.noContent().build();
    }

    //rotas específicas pra impressão de etiquetas
    @PreAuthorize("hasAuthority('copies:view')")
    @GetMapping("/etiquetas")
    public ResponseEntity<?> printLabels(@RequestParam List<Long> labels){
        auditLogService.record(AuditAction.ACCESS,"reipressão de etiquetas: " + labels);
        return ResponseEntity.ok(copyService.findLabels(labels));
    }
}
