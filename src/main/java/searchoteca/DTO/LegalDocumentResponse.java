package searchoteca.DTO;

/** Conteúdo em Markdown, para o frontend renderizar. */
public record LegalDocumentResponse(String title, String version, String content) {
}
