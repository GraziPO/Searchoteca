package searchoteca.DTO.archive;

import java.util.List;

public record LoanRequest(
        String ra,
        List<Long> items
) {
}
