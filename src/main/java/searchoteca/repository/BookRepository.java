package searchoteca.repository;

import org.springframework.data.repository.*;
import org.springframework.stereotype.Repository;
import searchoteca.model.BookModel;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookRepository extends CrudRepository<BookModel,String>{

    BookModel findByIsbn(String Isbn);

    void deleteByIsbn(String isbn);

}
