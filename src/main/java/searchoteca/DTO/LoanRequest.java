package searchoteca.DTO;

import java.util.List;

public record LoanRequest(
        String ra,
        List<Long> items
) {
}
