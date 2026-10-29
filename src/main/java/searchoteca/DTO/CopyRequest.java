package searchoteca.DTO;

public record CopyRequest(
        String isbn,
        String departCode,
        String localCode,
        int quantity
) {
}
