package searchoteca.service;

import org.springframework.stereotype.Service;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.repository.BookRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository){
        this.bookRepository=bookRepository;
    }

    public List<BookModel> findAll(){
        List<BookModel> books;
        books = (List<BookModel>) bookRepository.findAll();

        if(books.isEmpty()) {
            throw new ResourceNotFoundException("Nenhum livro cadastrado");
        }
        return books;
    }

    public BookModel create(BookModel book){
        return bookRepository.save(book);
    }

    public BookModel update (String isbn, BookModel bookInfo){
        BookModel book = bookRepository.findByIsbn(isbn);

        if (book == null){
            throw new ResourceNotFoundException("registro não encontrado");
        }
        book.setTitle(bookInfo.getTitle());
        book.setAuthor(bookInfo.getAuthor());
        book.setRel_year((bookInfo.getRel_year()));
        book.setPublisher(bookInfo.getPublisher());
        book.setGenre(bookInfo.getGenre());
        book.setDepartCode(bookInfo.getDepartCode());
        book.setLocalCode(bookInfo.getLocalCode());
        return bookRepository.save(book);
    }

    public void delete (String isbn){
        BookModel book = bookRepository.findByIsbn(isbn);
        if (book == null) {
            throw new ResourceNotFoundException("Nenhum livro encontrado");
        }
        bookRepository.deleteByIsbn(book.getIsbn());
    }

    public BookModel findByIsbn(String isbn){
        if (isbn == null){
            throw new ResourceNotFoundException("Nenhum livro encontrado");
        }
        return bookRepository.findByIsbn(isbn);
    }
}
