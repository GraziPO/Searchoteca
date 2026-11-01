package searchoteca.service.catalog;

import org.springframework.stereotype.Service;
import searchoteca.DTO.archive.publicSearch.BookDetail;
import searchoteca.DTO.archive.publicSearch.BookSearchResult;
import searchoteca.DTO.archive.publicSearch.PageResponse;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.BookModel;
import searchoteca.repository.archive.CatalogRepository;
import searchoteca.repository.catalog.BookRepository;

import java.util.List;

@Service
public class CatalogService {
    private static final int MAX_PAGE_SIZE = 20;

    private final CatalogRepository catalogRepository;
    private final BookRepository bookRepository;

    public CatalogService(CatalogRepository catalogRepository,  BookRepository bookRepository) {
        this.catalogRepository = catalogRepository;
        this.bookRepository = bookRepository;
    }

    public PageResponse<BookSearchResult> search(String q, int page, int size){
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page,0);

        String pattern = "%" + escapeLike(q==null ? "" : q.trim()) + "%";

        long total = catalogRepository.count(pattern);
        List<BookSearchResult> content = total == 0
                ? List.of()
                : catalogRepository.search(pattern,safeSize,safePage * safeSize);
        int totalPages = (int) Math.ceil((double)total / safeSize);

        return new PageResponse<>(content,safePage,safeSize,total,totalPages);
    }

    public BookDetail detail(String isbn){
        BookModel book = bookRepository.findByIsbn(isbn);

        if(book == null){
            throw new ResourceNotFoundException("registro não encontrado");
        }

        return new BookDetail(book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getGenre(),
                book.getRel_year(),
                catalogRepository.findAvailability(isbn));
    }

    private static String escapeLike(String s){
        return s.replaceAll("\\", "\\\\").replace("%","\\%").replace("_","\\_");
    }
}
