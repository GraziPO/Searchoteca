package searchoteca.service;

import org.springframework.stereotype.Service;
import searchoteca.model.LoanModel;
import searchoteca.repository.LoanRepository;

import java.util.List;

@Service
public class StudentLoanService {
    private final LoanRepository loanRepository;

    public StudentLoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public LoanModel findAllByUser_id(String user_id) {
        return (LoanModel) loanRepository.findByUser_id(user_id);
    }

}
