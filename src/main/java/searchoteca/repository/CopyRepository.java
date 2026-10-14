package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.CopyModel;

import java.util.List;

@Repository
public interface CopyRepository extends CrudRepository<CopyModel,Long> {

    CopyModel findByCopy_Id(Long id);
    List<CopyModel> findByDepartCode(Long departCode);
    List<CopyModel> findByLocalCode(Long localCode);

    List<CopyModel> findByIsbn(String isbn);

    void deleteByCustomCode(Long customId);


}

