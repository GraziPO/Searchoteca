package searchoteca.v0.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.v0.model.BookModel;
import searchoteca.v0.repository.BookRepository;

@RestController
@RequestMapping("/livro")
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository){
        this.bookRepository=bookRepository;
    };

    @GetMapping
    public ResponseEntity<?> getAll(){
        return ResponseEntity.ok(bookRepository.findAll());
    };

    @GetMapping("/{isbn}")
    public ResponseEntity<?> getByIsbn(@PathVariable int isbn){
        BookModel book = bookRepository.findByIsbn(isbn);
        if (book == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(book);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BookModel Book){
        return ResponseEntity.ok(bookRepository.save(Book));
    };

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BookModel Book){
        return ResponseEntity.ok(bookRepository.save(Book));
    };

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){
        return ResponseEntity.noContent().build();
    };
}
