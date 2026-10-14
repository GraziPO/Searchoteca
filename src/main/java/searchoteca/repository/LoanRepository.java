package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanModel;

import java.util.List;

@Repository
public interface LoanRepository extends CrudRepository<LoanModel,Long> {
    List<LoanModel> findAll();

    LoanModel findByLoan_id(Long loan_id);

    List<LoanModel> findByUsername(Long username);
    List<LoanModel> findByUserid(Long user_id);

    LoanModel findByLoanId(Long id);
}
