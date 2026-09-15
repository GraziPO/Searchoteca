package searchoteca.v0.repository;

import org.springframework.data.repository.*;
import org.springframework.stereotype.Repository;
import searchoteca.v0.model.BookModel;

import java.util.List;

@Repository
public interface BookRepository extends CrudRepository<BookModel,String>{

    BookModel findByIsbn(String Isbn);

    void deleteByIsbn(String isbn);

    List<BookModel> findByDepartCode(String departCode);

    List<BookModel> findByLocalCode(String localCode);
}
