package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.CopyModel;

import java.util.List;

@Repository
public interface CopyRepository extends CrudRepository<CopyModel,String> {

    List<CopyModel> findByDepartCode(String departCode);
    List<CopyModel> findByLocalCode(String localCode);

    List<CopyModel> findByIsbn(String isbn);

    void deleteByCustomCode(String customId);


}

