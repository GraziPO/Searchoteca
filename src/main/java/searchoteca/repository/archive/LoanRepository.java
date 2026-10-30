package searchoteca.repository.archive;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanModel;

import java.util.List;

@Repository
public interface LoanRepository extends CrudRepository<LoanModel,Long> {
    List<LoanModel> findAll();

    List<LoanModel> findByStudentId(Long user_id);

    LoanModel findByLoanId(Long loan_id);
}
