package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanModel;

import java.util.List;

@Repository
public interface LoanRepository extends CrudRepository<LoanModel,String> {
    List<LoanModel> findAll();

    List<LoanModel> findByUsername(String username);

    LoanModel findById(Long id);
}
