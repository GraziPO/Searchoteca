package searchoteca.DTO.archive.SalesNExists;

import java.math.BigDecimal;

public record ReceiptItem(
        Long copyId,
        String copyCode,
        String isbn,
        String title,
        String author,
        BigDecimal unitPrice
) {
}
