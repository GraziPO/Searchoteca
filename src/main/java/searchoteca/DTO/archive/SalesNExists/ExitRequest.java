package searchoteca.DTO.archive.SalesNExists;

import searchoteca.model.Books.status.ExitType;

import java.util.List;

public record ExitRequest(
        ExitType type,
        List<Long> copyIds,
        String notes
) {
}
