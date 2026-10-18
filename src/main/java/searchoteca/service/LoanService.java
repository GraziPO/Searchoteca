package searchoteca.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.*;
import searchoteca.model.status.CopyStatus;
import searchoteca.model.status.LoanStatus;
import searchoteca.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanItemRepository loanItemRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CopyRepository copyRepository;
;

    public LoanService(LoanRepository loanRepository, LoanItemRepository loanItemRepository, UserRepository userRepository,  CopyRepository copyRepository, StudentRepository studentRepository) {
        this.loanRepository = loanRepository;
        this.loanItemRepository = loanItemRepository;
        this.userRepository = userRepository;
        this.copyRepository = copyRepository;
        this.studentRepository = studentRepository;
    }

    public List<LoanModel> findAll() {
        List<LoanModel> loans = loanRepository.findAll();
        return loans;
    }

    //specific get for loans by student
    public List<LoanModel> findAllByStudent(Long studentId) {
        return loanRepository.findByUser(studentId);
    }

    public List<LoanModel> findByUsername(String username) {
        UserModel user = userRepository.findByUsername(username);
        return loanRepository.findByUser(user.getId());
    }

    public LoanModel findById(Long id) {
        Optional<LoanModel> loan = loanRepository.findById(id);
        return loan.orElse(null);
    }

    public LoanModel create(LoanModel loan, String ra) {
        LocalDateTime date = LocalDateTime.now();
        StudentModel student = studentRepository.findByRa(ra);

        student.setLastActivity(date);
        loan.setStatus(String.valueOf(LoanStatus.OPEN));
        loan.setLoan_Date(date);

        studentRepository.save(student);

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

            CopyModel copy = copyRepository.findByCopyId(item.getCopyId());
            copy.setStatus(CopyStatus.AVAILABLE.name());
            copyRepository.save(copy);

            item.setReturned_to(operatorId);
            item.setReturned_at(date);
            loanItemRepository.save(item);
        }

        //change the Loan Status to (Canceled)
        loan.setStatus(LoanStatus.CANCELLED.name());

        //set student last activity
        UserModel user = userRepository.findByUsername(loan.getUser());
        StudentModel student = studentRepository.findByStudentId(user.getId());
        student.setLastActivity(date);

        studentRepository.save(student);
        return loanRepository.save(loan);
    }
}
