package searchoteca.repository.wms;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanItemModel;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanItemRepository extends CrudRepository<LoanItemModel,Long> {
    List<LoanItemModel> findByLoanId(Long loanId);

    @Query("SELECT * FROM org_loan_items WHERE copy_id = :copyId AND returned_at IS NULL")
    Optional<LoanItemModel> findOpenByCopyId(@Param("copyId") Long copyId);

    @Query("SELECT COUNT(*) FROM org_loan_items WHERE loan_id = :loanId AND returned_at IS NULL")
    long countOpenItemsByLoanId(@Param("loanId")Long loanId);
}
