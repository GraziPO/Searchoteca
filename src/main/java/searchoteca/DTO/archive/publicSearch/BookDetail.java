package searchoteca.DTO.archive.publicSearch;

import java.util.List;

public record BookDetail(
        String isbn,
        String title,
        String author,
        String publisher,
        String genre,
        Integer relYear,
        List<CopyAvaliability> locations
) {
}
