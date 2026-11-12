package searchoteca.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.archive.LoanRequest;
import searchoteca.DTO.archive.SalesNExists.ReceiptItem;
import searchoteca.DTO.users.student.StudentInfo;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.*;
import searchoteca.model.Books.BookModel;
import searchoteca.model.Books.CopyModel;
import searchoteca.model.Books.status.CopyStatus;
import searchoteca.model.Books.status.ExitType;
import searchoteca.model.Books.status.LoanStatus;
import searchoteca.repository.catalog.BookRepository;
import searchoteca.repository.users.UserRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.repository.users.StudentRepository;
import searchoteca.repository.archive.LoanItemRepository;
import searchoteca.repository.archive.LoanRepository;
import searchoteca.security.CustomUserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanItemRepository loanItemRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CopyRepository copyRepository;
    private final ReceiptService receiptService;
    private final BookRepository bookRepository;
    private final WaitlistService waitlistService;


    public LoanService(LoanRepository loanRepository,
                       LoanItemRepository loanItemRepository,
                       UserRepository userRepository,
                       CopyRepository copyRepository,
                       StudentRepository studentRepository,
                       ReceiptService receiptService,
                       BookRepository bookRepository,
                       WaitlistService waitlistService) {
        this.loanRepository = loanRepository;
        this.loanItemRepository = loanItemRepository;
        this.userRepository = userRepository;
        this.copyRepository = copyRepository;
        this.studentRepository = studentRepository;
        this.receiptService = receiptService;
        this.bookRepository = bookRepository;
        this.waitlistService = waitlistService;
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
    public LoanModel create(LoanRequest request, CustomUserDetails operator) {
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
        loan.setOperatorId(operator.getUserId());
        loan.setLoanDate(now);
        loan.setDueDate(now.plusDays(15)); //standard value just for testing. the period may be adjusted by an admin user in later versions.
        loan.setStatus(LoanStatus.OPEN.name());

        loan = loanRepository.save(loan);

        List<CopyModel> loaned = new ArrayList<>();

        for(Long copyId : request.items()){
            CopyModel copy = copyRepository.findById(copyId)
                    .orElseThrow(()-> new ResourceNotFoundException("Exemplar não existente"));

            if (CopyStatus.RESERVED.name().equals(copy.getStatus())) {
                // reservado pela fila de espera: só o aluno dono da reserva pode levar
                waitlistService.claimReservation(copy, student.getStudentId());

            } else if(!CopyStatus.AVAILABLE.name().equals(copy.getStatus())){
                throw new ResourceConflictException("Exemplar " + copy.getCopy_custom_code() + " não está disponível");
            }

            copy.setStatus(CopyStatus.LOANED.name());
            loaned.add(copy);

            copyRepository.save(copy);

            LoanItemModel item = new LoanItemModel();
            item.setLoanId(loan.getLoanId());
            item.setCopyId(copy.getCopyId());
            loanItemRepository.save(item);

            waitlistService.closeEntriesAfterLoan(student.getStudentId(), loaned);
        }

        Map<String, BookModel> books = bookRepository.findByIsbnIn(
                        loaned.stream().map(CopyModel::getIsbn).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(BookModel::getIsbn, b -> b));

        List<ReceiptItem> items = loaned.stream().map(c -> {
            BookModel b = books.get(c.getIsbn());
            return new ReceiptItem(c.getCopyId(), c.getCopy_custom_code(),
                    b.getIsbn(), b.getTitle(), b.getAuthor(), null);   // empréstimo não tem preço
        }).toList();

        UserModel studentUser = userRepository.findById(student.getUserId()).orElseThrow();
        receiptService.issue(ExitType.LOAN, operator,
                new StudentInfo(student.getRa(), studentUser.getCompleteName()),
                loan.getDueDate(), loan.getLoanId(), items, null);

        student.setLastActivity(now);

        studentRepository.save(student);

        receiptService.issue(ExitType.LOAN, operator, null, loan.getDueDate(), loan.getLoanId(), null, null);

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


    // Fecha o item e libera o exemplar: vai para o primeiro da fila de espera ou fica disponível.
    private void releaseItem(LoanItemModel item, Long operatorId, LocalDateTime now){
        CopyModel copy = copyRepository.findById(item.getCopyId())
                .orElseThrow(() -> new ResourceNotFoundException("Exemplar não existente"));

        item.setReturned_to(operatorId);
        item.setReturned_at(now);
        loanItemRepository.save(item);

        waitlistService.releaseCopy(copy);   // antes: copy.setStatus(AVAILABLE); save(copy)
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
