package searchoteca.DTO.dashboard;

import java.time.LocalDate;

public record Diary(
        LocalDate day,
        long loans,
        long returns
) {
}
