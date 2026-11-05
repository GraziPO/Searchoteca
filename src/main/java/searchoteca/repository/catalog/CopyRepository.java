package searchoteca.repository.catalog;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchoteca.model.Books.CopyModel;

import java.util.Collection;
import java.util.List;

@Repository
public interface CopyRepository extends CrudRepository<CopyModel,Long> {
    CopyModel findByCopyId(Long id);
    List<CopyModel> findByDepartCode(String departCode);
    List<CopyModel> findByLocalCode(String localCode);

    List<CopyModel> findByIsbn(String isbn);
    List<CopyModel> findAllByIsbn(String isbn);

    @Query("SELECT COALESCE(MAX(copy_index), 0) FROM org_copies WHERE isbn = :isbn")
    int findMaxIndexByIsbn(@Param("isbn")String isbn);

    long countByIsbn(String isbn);

    @Query("SELECT * FROM org_copies WHERE copy_custom_code IN (:codes")
    List<CopyModel> findByCustomCodes(@Param("codes") Collection<String> codes);
}

