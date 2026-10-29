package searchoteca.repository;

import org.springframework.data.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchoteca.model.BookModel;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookRepository extends CrudRepository<BookModel,Long>{

    BookModel findByIsbn(String Isbn);

    void deleteByIsbn(String isbn);

    List<BookModel> findByIsbnIn(Collection<String> isbn);

}
