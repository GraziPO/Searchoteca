package searchoteca.v0.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchoteca.v0.model.BookModel;
import searchoteca.v0.model.DepartmentModel;
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
    public ResponseEntity<?> getByIsbn(@PathVariable String isbn){
        BookModel book = bookRepository.findByIsbn(isbn);
        if (book == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(book);
    }

    @PostMapping("/{isbn}")
    public ResponseEntity<?> update(@PathVariable String isbn, @RequestBody BookModel Book){
        return ResponseEntity.ok(bookRepository.save(Book));
    };

    @PutMapping("/{isbn}")
    public ResponseEntity<?> create(@PathVariable String isbn, @RequestBody BookModel bookInfo){
        BookModel book = bookRepository.findByIsbn(isbn);
        book.setIsbn(isbn);
        book.setTitle(bookInfo.getTitle());
        book.setAuthor(bookInfo.getAuthor());
        book.setRel_year((bookInfo.getRel_year()));
        book.setPublisher(bookInfo.getPublisher());
        book.setGenre(bookInfo.getGenre());
        return ResponseEntity.ok(bookRepository.save(book));
    };

    @DeleteMapping("/{isbn}")
    public ResponseEntity<?> delete(@PathVariable String isbn) {
        BookModel book = bookRepository.findByIsbn(isbn);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        bookRepository.deleteByIsbn(book.getIsbn());
        return ResponseEntity.noContent().build();
    };
}
