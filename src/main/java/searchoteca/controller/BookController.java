package searchoteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.model.BookModel;
import searchoteca.service.BookService;

@RestController
@RequestMapping("/api/livro")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService){
        this.bookService=bookService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(){
        return ResponseEntity.ok(bookService.findAll());
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<?> getByIsbn(@PathVariable String isbn){
        BookModel book = bookService.findByIsbn(isbn);
        if (book == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(book);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BookModel Book){
        return ResponseEntity.ok(bookService.create(Book));
    }

    @PutMapping("/{isbn}")
    public ResponseEntity<?> update(@PathVariable String isbn, @RequestBody BookModel bookInfo){
        return ResponseEntity.ok(bookService.update(isbn, bookInfo));
    }

    @DeleteMapping("/{isbn}")
    public ResponseEntity<?> delete(@PathVariable String isbn) {
        bookService.delete(isbn);
        return ResponseEntity.noContent().build();
    }
}
