package searchoteca.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.archive.LoanRequest;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.*;
import searchoteca.model.status.CopyStatus;
import searchoteca.model.status.LoanStatus;
import searchoteca.repository.users.UserRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.repository.users.StudentRepository;
import searchoteca.repository.wms.LoanItemRepository;
import searchoteca.repository.wms.LoanRepository;

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


    public LoanService(LoanRepository loanRepository, LoanItemRepository loanItemRepository, UserRepository userRepository, CopyRepository copyRepository, StudentRepository studentRepository) {
        this.loanRepository = loanRepository;
        this.loanItemRepository = loanItemRepository;
        this.userRepository = userRepository;
        this.copyRepository = copyRepository;
        this.studentRepository = studentRepository;
    }

    public List<LoanModel> findAll() {
        return loanRepository.findAll();
    }

    //specific get for loans by student
    public List<LoanModel> findAllByStudent(Long studentId) {
        UserModel user = userRepository.findByUserId(studentId);
        StudentModel student = studentRepository.findByUserId(user.getUserId());

        return loanRepository.findByStudentId(student.getStudentId());
    }

    public LoanModel findById(Long id) {
        Optional<LoanModel> loan = loanRepository.findById(id);
        return loan.orElse(null);
    }

    @Transactional
    public LoanModel create(LoanRequest request, Long operatorId) {
        StudentModel student = studentRepository.findByRa(request.ra());
        if(student == null) {
            throw new ResourceNotFoundException("Aluno não encontrado");
        }
        if(request.items() == null || request.items().isEmpty()){
            throw new ResourceConflictException("INforme ao menos um exemplar");
        }

        LocalDateTime now = LocalDateTime.now();
        LoanModel loan = new LoanModel();
        loan.setStudentId(student.getStudentId());
        loan.setOperatorId(operatorId);
        loan.setLoanDate(now);
        loan.setDueDate(now.toLocalDate().plusDays(15)); //standard value just for testing. the period may be adjusted by an admin user in later versions.
        loan.setStatus(LoanStatus.OPEN.name());

        loan = loanRepository.save(loan);

        for(Long copyId : request.items()){
            CopyModel copy = copyRepository.findById(copyId)
                    .orElseThrow(()-> new ResourceNotFoundException("Exemplar não existente"));
            if(!CopyStatus.AVAILABLE.name().equals(copy.getStatus())){
                throw new ResourceConflictException("Esemplar não disponivel");
            }

            copy.setStatus(CopyStatus.LOANED.name());
            copyRepository.save(copy);

            LoanItemModel item = new LoanItemModel();
            item.setLoanId(loan.getLoanId());
            item.setCopyId(copy.getCopyId());
            loanItemRepository.save(item);
        }

        student.setLastActivity(now);
        studentRepository.save(student);
        return loan;
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
        StudentModel student = studentRepository.findByStudentId(loan.getStudentId());
        student.setLastActivity(date);

        studentRepository.save(student);
        return loanRepository.save(loan);
    }


    private void releaseItem(LoanItemModel item, Long operatorId, LocalDateTime now){
        CopyModel copy = copyRepository.findById(item.getCopyId())
                .orElseThrow(() -> new ResourceNotFoundException("Exemplar não existente"));
        copy.setStatus(CopyStatus.AVAILABLE.name());
        copyRepository.save(copy);

        item.setReturned_to(operatorId);
        item.setReturned_at(now);
        loanItemRepository.save(item);
    }

    @Transactional
    public LoanModel returnItem(Long copyId, Long operatorId){
        LoanItemModel item = loanItemRepository.findOpenByCopyId(copyId)
                .orElseThrow(()-> new ResourceConflictException("Não existe registro de empréstimo relacionado a esta cópia"));

        LoanModel loan = loanRepository.findById(item.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado"));
        if(!LoanStatus.OPEN.name().equals(loan.getStatus())) {
            throw new ResourceConflictException("Registro não está aberto");
        }


        LocalDateTime now = LocalDateTime.now();

        releaseItem(item, operatorId, now);

        if(loanItemRepository.countOpenItemsByLoanId(loan.getLoanId()) == 0){
            loan.setStatus(LoanStatus.CLOSED.name());
        }

        StudentModel student = studentRepository.findByStudentId(loan.getStudentId());
        if(student != null){
            student.setLastActivity(now);
            studentRepository.save(student);
        }
        return loanRepository.save(loan);
    }
}
