package searchoteca.DTO.archive.publicSearch;

public record CopyAvaliability(
        String departCode,
        String departName,
        String localCode,
        String localName,
        int available,
        int total
) {
}
