package searchoteca.service;

import org.springframework.stereotype.Service;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.model.CopyModel;
import searchoteca.repository.BookRepository;
import searchoteca.repository.CopyRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;

    public BookService(BookRepository bookRepository, CopyRepository copyRepository) {
        this.bookRepository=bookRepository;
        this.copyRepository=copyRepository;
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

    public List<BookModel> findLocations(String isbn){
        if (isbn == null){
            throw new ResourceNotFoundException("Nenhum registro encontrado");
        }
        //fetches all the location codes linked to the book, and return them
        List<String> isbnList = copyRepository.findByIsbn(isbn)
                .stream()
                .map(CopyModel::getIsbn)
                .distinct()
                .toList();

        List<BookModel> foundBooks =  new ArrayList<>();

        for (String book : isbnList){
            foundBooks.add(bookRepository.findByIsbn(book));
        }
        return foundBooks;
    }
}
