package searchoteca.DTO.legal;

/** Conteúdo em Markdown, para o frontend renderizar. */
public record LegalDocumentResponse(String title, String version, String content) {
}
