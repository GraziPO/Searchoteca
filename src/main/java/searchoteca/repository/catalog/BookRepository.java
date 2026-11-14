package searchoteca.repository.catalog;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.*;
import org.springframework.stereotype.Repository;
import searchoteca.model.Books.BookModel;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookRepository extends CrudRepository<BookModel,Long>, PagingAndSortingRepository<BookModel,Long>{

    BookModel findByIsbn(String Isbn);

    void deleteByIsbn(String isbn);

    List<BookModel> findByIsbnIn(Collection<String> isbn);

}
