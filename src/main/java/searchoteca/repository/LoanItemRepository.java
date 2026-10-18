package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanItemModel;

import java.util.List;

@Repository
public interface LoanItemRepository extends CrudRepository<LoanItemModel,Long> {
    List<LoanItemModel> findByLoanId(Long loanId);
    LoanItemModel findByItemId(Long id);
    LoanItemModel findByCopyId(Long itemId);
}
