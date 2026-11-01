package searchoteca.DTO.archive;

public record CopyLabel (
    Long copyId,
    String code,
    String title,
    String author,
    String departCode,
    String localCode
    ){}
