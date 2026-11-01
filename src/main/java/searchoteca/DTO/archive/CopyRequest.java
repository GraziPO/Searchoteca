package searchoteca.DTO.archive;

public record CopyRequest(
        String isbn,
        String departCode,
        String localCode,
        int quantity
) {
}
