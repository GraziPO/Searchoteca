package searchoteca.DTO.archive.publicSearch;

public record BookSearchResult(
        String isbn,
        String title,
        String author,
        String publisher,
        String genre,
        Integer relYear,
        int totalCopies,
        int availableCopies
) {
}
