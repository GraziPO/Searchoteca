package searchoteca.repository.archive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanModel;

import java.util.List;

@Repository
public interface LoanRepository extends CrudRepository<LoanModel,Long>, PagingAndSortingRepository<LoanModel,Long> {
    List<LoanModel> findAll();

    List<LoanModel> findByStudentId(Long user_id);

    /** Histórico do aluno, paginado (o Spring Data também faz a contagem do total). */
    Page<LoanModel> findByStudentId(Long studentId, Pageable pageable);

    LoanModel findByLoanId(Long loan_id);
}
