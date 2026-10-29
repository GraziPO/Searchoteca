package searchoteca.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchoteca.model.CopyModel;

import java.util.List;

@Repository
public interface CopyRepository extends CrudRepository<CopyModel,Long> {
    CopyModel findByCopyId(Long id);
    List<CopyModel> findByDepartCode(String departCode);
    List<CopyModel> findByLocalCode(String localCode);
    List<CopyModel> findAllByIsbn(String isbn);

    @Query("COALESCE(MAX(copy_index), 0) FROM org_copies WHERE isbn = :isbn")
    int findMaxIndexByIsbn(@Param("isbn")String isbn);

    long countByIsbn(String isbn);
}

