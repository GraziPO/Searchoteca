package searchoteca.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
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

    //specific get for loans by student
    public LoanModel findAllByStudent(Long studentId) {
        List<LoanModel> loans =  loanRepository.findByUserid(studentId);
        return (LoanModel) loans;
    }

    public List<LoanModel> findByUsername(String username) {
        UserModel user = userRepository.findByUsername(username);
        List<LoanModel> loans = loanRepository.findByUserid(user.getId());
        return loans;
    }

    public LoanModel findById(Long id) {
        LoanModel loan = loanRepository.findByLoanId(id);
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
        LoanModel loan = loanRepository.findByLoanId(loanId);
        if (loan == null) {
            throw new ResourceNotFoundException("Registro não encontrado");
        }

        if(!LoanStatus.OPEN.name().equals(loan.getStatus())) {
            throw new ResourceConflictException("impossível realizar operação. Verifique o status do pedido.");
        }
        LocalDateTime date = LocalDateTime.now();

        for(LoanItemModel item: loanItemRepository.findByLoanId(loanId)){

            if(item.getReturned_at() != null){
                continue;
            }

            CopyModel copy = copyRepository.findByCopy_Id(item.getCopy_id());
            copy.setStatus(CopyStatus.AVAILABLE.name());
            copyRepository.save(copy);

            item.setReturned_to(operatorId);
            item.setReturned_at(date);
            loanItemRepository.save(item);
        }

        //change the Loan Status to (Canceled)
        loan.setStatus(LoanStatus.CANCELLED.name());
        return loanRepository.save(loan);
    }
}
