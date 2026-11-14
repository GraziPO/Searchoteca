package searchoteca.DTO.archive;

/**
 * Dados encontrados para um ISBN, já no formato do cadastro de livro (mesmos nomes do BookModel):
 * o frontend preenche o formulário com isso, o operador confere/ajusta e envia o POST /api/livro.
 * Campos que a Open Library não tem vêm null.
 */
public record IsbnLookupResult(
        String isbn,
        String title,
        String author,
        Integer rel_year,
        String publisher,
        String genre,
        String source) {
}
