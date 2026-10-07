package searchoteca.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.model.CopyModel;
import searchoteca.model.LoanItemModel;
import searchoteca.model.LoanModel;
import searchoteca.model.UserModel;
import searchoteca.model.status.CopyStatus;
import searchoteca.model.status.LoanStatus;
import searchoteca.repository.CopyRepository;
import searchoteca.repository.LoanItemRepository;
import searchoteca.repository.LoanRepository;
import searchoteca.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanItemRepository loanItemRepository;
    private final UserRepository userRepository;
    private final CopyRepository copyRepository;
;

    public LoanService(LoanRepository loanRepository, LoanItemRepository loanItemRepository, UserRepository userRepository,  CopyRepository copyRepository) {
        this.loanRepository = loanRepository;
        this.loanItemRepository = loanItemRepository;
        this.userRepository = userRepository;
        this.copyRepository = copyRepository;
    }

    public List<LoanModel> findAll() {
        List<LoanModel> loans = loanRepository.findAll();
        return loans;
    }

    public List<LoanModel> findByUsername(String username) {
        UserModel user = userRepository.findByUsername(username);
        List<LoanModel> loans = loanRepository.findByUser_id(user.getId());
        return loans;
    }

    public LoanModel findById(String id) {
        LoanModel loan = loanRepository.findById(id).orElse(null);
        return loan;
    }

    public LoanModel create(LoanModel loan) {
        LocalDateTime date = LocalDateTime.now();

        loan.setStatus(String.valueOf(LoanStatus.OPEN));
        loan.setLoan_Date(date);

        return loanRepository.save(loan);
    }

    @Transactional
    public LoanModel cancelLoan(Long loanId, Long operatorId) {
        LocalDateTime date = LocalDateTime.now();

        LoanModel loan = loanRepository.findById(loanId);
        List<String> items = loanItemRepository.findByLoanId(loanId)
                .stream()
                .map(LoanItemModel ::getCopy_id)
                .distinct()
                .toList();

        //set status of each loaned copy back to default (AVAILABLE)
         for (String item : items) {
             CopyModel copy = (CopyModel) copyRepository.findByCopy_id(item);
             copy.setStatus(String.valueOf(CopyStatus.AVAILABLE));
         }

         //change the Loan Status to (Canceled)
        loan.setStatus(String.valueOf(LoanStatus.CANCELLED));
         return loanRepository.save(loan);
    }
}
