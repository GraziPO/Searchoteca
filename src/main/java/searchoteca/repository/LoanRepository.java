package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanModel;

import java.util.List;

@Repository
public interface LoanRepository extends CrudRepository<LoanModel,String> {
    List<LoanModel> findAll();

    LoanModel findByLoan_id(String loan_id);

    List<LoanModel> findByUsername(String username);
    List<LoanModel> findByUser_id(Long user_id);

    LoanModel findById(Long id);
}
