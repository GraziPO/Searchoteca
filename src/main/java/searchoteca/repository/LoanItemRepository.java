package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.LoanItemModel;

import java.util.List;

@Repository
public interface LoanItemRepository extends CrudRepository<LoanItemModel,Long> {
    List<LoanItemModel> findByLoanId(Long loanId);
    LoanItemModel findByItem_Id(Long id);
    LoanItemModel findByCopy_Id(Long itemId);
}
