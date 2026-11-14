package searchoteca.service.catalog;

import org.springframework.data.domain.Sort;
import searchoteca.DTO.archive.publicSearch.PageResponse;
import searchoteca.components.Paging;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditDiff;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.Books.BookModel;
import searchoteca.repository.catalog.BookRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.service.settings.SettingsService;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;
    private final AuditLogService auditLogService;
    private final SettingsService settingsService;

    public BookService(BookRepository bookRepository, CopyRepository copyRepository, AuditLogService auditLogService, SettingsService settingsService) {
        this.bookRepository=bookRepository;
        this.copyRepository=copyRepository;
        this.auditLogService=auditLogService;
        this.settingsService=settingsService;
    }

    public List<BookModel> findAll(){
        List<BookModel> books;
        books = (List<BookModel>) bookRepository.findAll();

        return books;   // lista vazia é resposta válida, não erro
    }

    /** Listagem paginada, em ordem de título (RNF06). */
    public PageResponse<BookModel> findPage(int page, int size){
        return PageResponse.from(bookRepository.findAll(Paging.of(page, size, Sort.by("title"))));
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
        AuditDiff diff = new AuditDiff()
                .field("título", book.getTitle(), bookInfo.getTitle())
                .field("autor", book.getAuthor(), bookInfo.getAuthor())
                .field("ano", book.getRel_year(), bookInfo.getRel_year())
                .field("editora", book.getPublisher(), bookInfo.getPublisher())
                .field("gênero", book.getGenre(), bookInfo.getGenre());

        book.setTitle(bookInfo.getTitle());
        book.setAuthor(bookInfo.getAuthor());
        book.setRel_year((bookInfo.getRel_year()));
        book.setPublisher(bookInfo.getPublisher());
        book.setGenre(bookInfo.getGenre());
        BookModel saved = bookRepository.save(book);

        auditLogService.record(AuditAction.UPDATE, "livro " + isbn + ": " + diff);
        return saved;
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

    // ================== exclusive service to sales functions ====================

    @Transactional
    public BookModel updatePrice(String isbn, BigDecimal newPrice){
        if(!settingsService.salesEnabled()){   // ligado/desligado pelo root
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
        auditLogService.record(AuditAction.UPDATE,"preço do livro " + isbn + ": " + diff);
        return book;
    }
}
