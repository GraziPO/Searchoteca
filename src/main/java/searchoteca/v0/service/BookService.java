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
    };

    public List<BookModel> findAll(){
        return (List<BookModel>) bookRepository.findAll();
    };

    public BookModel create(BookModel book){
        return bookRepository.save(book);
    };

    public BookModel update (BookModel book){
        return bookRepository.save(book);
    };

    public void delete (BookModel book){
        bookRepository.delete(book);
    };
}
