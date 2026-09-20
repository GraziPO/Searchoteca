package searchoteca.v0.service;

import org.springframework.stereotype.Service;
import searchoteca.v0.model.BookModel;
import searchoteca.v0.repository.BookRepository;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository){
        this.bookRepository=bookRepository;
    }

    public List<BookModel> findAll(){
        return (List<BookModel>) bookRepository.findAll();
    }

    public BookModel create(BookModel book){
        return bookRepository.save(book);
    }

    public BookModel update (String isbn, BookModel bookInfo){
        BookModel book = bookRepository.findByIsbn(isbn);
        book.setTitle(bookInfo.getTitle());
        book.setAuthor(bookInfo.getAuthor());
        book.setRel_year((bookInfo.getRel_year()));
        book.setPublisher(bookInfo.getPublisher());
        book.setGenre(bookInfo.getGenre());
        return bookRepository.save(book);
    }

    public void delete (String isbn){
        BookModel book = bookRepository.findByIsbn(isbn);
        if (book == null) {
            return;
        }
        bookRepository.deleteByIsbn(book.getIsbn());
    }

    public BookModel findByIsbn(String isbn){
        return bookRepository.findByIsbn(isbn);
    }
}
