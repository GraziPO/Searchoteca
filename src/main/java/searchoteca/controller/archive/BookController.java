package searchoteca.controller.archive;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.Books.BookModel;
import searchoteca.service.catalog.BookService;
import searchoteca.service.catalog.CopyService;

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

    @PreAuthorize("hasAuthority('org_books:view')")
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
        bookService.update(isbn, bookInfo);
        auditLogService.record(AuditAction.UPDATE,"org_books" + isbn + " " + bookInfo);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('org_books:delete')")
    @DeleteMapping("/{isbn}")
    public ResponseEntity<?> delete(@PathVariable String isbn) {
        bookService.delete(isbn);
        auditLogService.record(AuditAction.DELETE,"org_books" + isbn);
        return ResponseEntity.noContent().build();
    }

    //rota de alteração de preço
    @PreAuthorize("hasAuthority('org_books:edit_price')")
    @PostMapping("/{isbn}/preco")
    public ResponseEntity<?> updatePrice(@PathVariable String isbn, @RequestBody PriceRequest request){
        return ResponseEntity.ok(bookService.updatePrice(isbn, request.salePrice()));
    }
}
