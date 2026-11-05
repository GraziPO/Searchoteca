package searchoteca.DTO.archive.SalesNExists;

import searchoteca.DTO.users.operator.OperatorInfo;
import searchoteca.DTO.users.student.StudentInfo;
import searchoteca.model.Books.status.ExitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReceiptSnapshot(
        String code,
        ExitType exitType,
        LocalDateTime issuedAt,
        OperatorInfo operator,
        StudentInfo student,
        LocalDate dueDate,
        List<ReceiptItem> items,
        BigDecimal total,
        String notes
) {
}
