package searchoteca.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import searchoteca.DTO.archive.SalesNExists.ReceiptItem;
import searchoteca.DTO.archive.SalesNExists.ReceiptSnapshot;
import searchoteca.DTO.users.operator.OperatorInfo;
import searchoteca.DTO.users.student.StudentInfo;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.Books.status.ExitType;
import searchoteca.repository.catalog.BookRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.repository.receipts.ReceiptRepository;
import searchoteca.security.CustomUserDetails;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final JsonMapper jsonMapper;


    public ReceiptService(ReceiptRepository receiptRepository, JsonMapper jsonMapper, CopyRepository copyRepository, BookRepository bookRepository) {
        this.receiptRepository = receiptRepository;
        this.jsonMapper = jsonMapper;
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ReceiptSnapshot issue(ExitType type, CustomUserDetails operator, StudentInfo student, LocalDate dueDate, Long loanId, List<ReceiptItem> items, String notes) {
        LocalDateTime now  = LocalDateTime.now();
        long id = receiptRepository.nextId();
        String code = "CMP-" + now.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" String.format("%06d", id);

        BigDecimal total = items.stream()
                .map(ReceiptItem::unitPrice).filter(Objects::nonNull)
                .reduce(BigDecimal::add).orElse(null);

        ReceiptSnapshot snapshot = new ReceiptSnapshot(code, type, now,
                new OperatorInfo(operator.getUserId(), operator.getUsername(), operator.getCompleteName()),
                student, dueDate, items, total, notes);

        receiptRepository.insert(id, code, type, operator.getUserId(), loanId, total, now, jsonMapper.writeValueAsString(snapshot));

        for (ReceiptItem item : items) {
            receiptRepository.insertItem((id, item.copyId(),item.unitPrice());
        }

        return snapshot;
    }

    public String findByCode(String code){
        return receiptRepository.findSnapshotByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Comprovante não encontrado"));
    }
}
