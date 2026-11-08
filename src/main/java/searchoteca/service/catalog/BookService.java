package searchoteca.service.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditDiff;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.Books.BookModel;
import searchoteca.model.Books.CopyModel;
import searchoteca.repository.catalog.BookRepository;
import searchoteca.repository.catalog.CopyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;
    private final AuditLogService auditLogService;
    private final boolean salesEnabled;

    public BookService(BookRepository bookRepository, CopyRepository copyRepository, AuditLogService auditLogService, @Value("${sales.enabled:false}")  boolean salesEnabled) {
        this.bookRepository=bookRepository;
        this.copyRepository=copyRepository;
        this.auditLogService=auditLogService;
        this.salesEnabled=salesEnabled;
    }

    public List<BookModel> findAll(){
        List<BookModel> books;
        books = (List<BookModel>) bookRepository.findAll();

        return books;   // lista vazia é resposta válida, não erro
    }

    public BookModel create(BookModel book){
        book.setSalePrice(null);
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
        if (!copyRepository.findByIsbn(isbn).isEmpty()) {
            throw new ResourceConflictException("Não é possível excluir: este livro tem exemplares cadastrados.");
        }
        bookRepository.deleteByIsbn(book.getIsbn());
    }

    public BookModel findByIsbn(String isbn){
        if (isbn == null){
            throw new ResourceNotFoundException("Nenhum livro encontrado");
        }
        return bookRepository.findByIsbn(isbn);
    }

    public List<BookModel> findLocationByIsbn(String isbn){
        if (isbn == null){
            throw new ResourceNotFoundException("Nenhum registro encontrado");
        }
        //fetches all the location codes linked to the book, and return them
        List<String> isbnList = copyRepository.findByIsbn(isbn)
                .stream()
                .map(CopyModel::getLocalCode)
                .distinct()
                .toList();

        List<BookModel> foundBooks =  new ArrayList<>();

        for (String book : isbnList){
            foundBooks.add(bookRepository.findByIsbn(book));
        }
        return foundBooks;
    }

    // ================== exclusive service to sales functions ====================

    @Transactional
    public BookModel updatePrice(String isbn, BigDecimal newPrice){
        if(!salesEnabled){
            throw new ResourceConflictException("Vendas não habilitado para esta organização");
        }
        BookModel book = bookRepository.findByIsbn(isbn);
        if(book == null){
            throw new ResourceNotFoundException("Nenhum livro encontrado");
        }
        if((newPrice != null) && (newPrice.signum() < 0)){
            throw new ResourceConflictException("O preço não pode ser negativo");
        }

        BigDecimal normalized = newPrice == null ? null : newPrice.setScale(2, RoundingMode.HALF_UP);
        String diff = new AuditDiff().field("salePrice", book.getSalePrice(), normalized).toString();

        book.setSalePrice(normalized);
        bookRepository.save(book);
        auditLogService.record(AuditAction.UPDATE,"preço do livro"+ isbn + ": " + diff);
        return book;
    }
}
