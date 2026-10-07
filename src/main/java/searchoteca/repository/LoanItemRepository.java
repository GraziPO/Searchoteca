package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanItemModel;

import java.util.List;

@Repository
public interface LoanItemRepository extends CrudRepository<LoanItemModel,String> {
    List<LoanItemModel> findByLoanId(Long loanId);
    LoanItemModel findById(Long id);
    LoanItemModel findByCopyId(Long itemId);
}
