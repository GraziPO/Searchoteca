package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CatalogController (/api/public/acervo): consulta pública do acervo, sem login
 * ("consulta ao acervo pode ser feita sem necessidade de criação de usuário").
 */
class CatalogControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/public/acervo";

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> conteudo(Map<String, Object> pagina) {
        return (List<Map<String, Object>>) pagina.get("content");
    }

    // ----------------------------------------------------------------- GET /api/public/acervo

    @Test
    void search_semToken_comTermoDoTitulo_deveRetornar200ComContagemDeExemplares() {
        // Arrange
        String termo = "busca" + uid();
        String departCode = (String) criarDepartamento().get("departCode");
        String localCode = (String) criarLocalizacao(departCode).get("localCode");
        String isbn = (String) criarLivro("Livro " + termo).get("isbn");
        criarExemplares(isbn, departCode, localCode, 2);

        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "?q=" + termo, null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("page", 0)
                .containsEntry("size", 20)
                .containsEntry("totalElements", 1)
                .containsEntry("totalPages", 1);
        List<Map<String, Object>> livros = conteudo(resposta.getBody());
        assertThat(livros).hasSize(1);
        assertThat(livros.get(0))
                .containsEntry("isbn", isbn)
                .containsEntry("title", "Livro " + termo)
                .containsEntry("relYear", 2020)
                .containsEntry("totalCopies", 2)
                .containsEntry("availableCopies", 2);
    }

    @Test
    void search_semResultado_deveRetornar200ComPaginaVazia() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "?q=inexistente" + uid(), null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("totalElements", 0)
                .containsEntry("totalPages", 0);
        assertThat(conteudo(resposta.getBody())).isEmpty();
    }

    @Test
    void search_comPaginaNegativaETamanhoAcimaDoLimite_deveAjustarParaPagina0ETamanho20() {
        // Arrange
        criarLivro();

        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "?page=-3&size=500", null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("page", 0)
                .containsEntry("size", 20);
        assertThat(conteudo(resposta.getBody())).isNotEmpty().hasSizeLessThanOrEqualTo(20);
    }

    // ----------------------------------------------------------------- GET /api/public/acervo/{isbn}

    @Test
    void bookDetail_comExemplarEmprestado_deveRetornarDisponibilidadePorLocalizacao() {
        // Arrange - 2 exemplares na mesma localização, 1 deles emprestado
        Map<String, Object> departamento = criarDepartamento();
        String departCode = (String) departamento.get("departCode");
        Map<String, Object> localizacao = criarLocalizacao(departCode);
        String localCode = (String) localizacao.get("localCode");
        String isbn = (String) criarLivro().get("isbn");
        List<Map<String, Object>> exemplares = criarExemplares(isbn, departCode, localCode, 2);
        Aluno aluno = criarAluno();
        ResponseEntity<Map<String, Object>> emprestimo = post("/api/emprestimos", tokenDoPerfil("loan_op"),
                Map.of("ra", aluno.ra(), "items", List.of(exemplares.get(0).get("copyId"))));
        assertThat(emprestimo.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/" + isbn, null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("isbn", isbn)
                .containsEntry("author", "Autor de Teste")
                .containsEntry("relYear", 2020);
        List<Map<String, Object>> locais = conteudoLocais(resposta.getBody());
        assertThat(locais).hasSize(1);
        assertThat(locais.get(0))
                .containsEntry("departCode", departCode)
                .containsEntry("departName", departamento.get("departName"))
                .containsEntry("localCode", localCode)
                .containsEntry("localName", localizacao.get("localName"))
                .containsEntry("available", 1)
                .containsEntry("total", 2);
    }

    @Test
    void bookDetail_comIsbnInexistente_deveRetornar404ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/" + isbnUnico(), null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody())
                .containsEntry("status", 404)
                .containsEntry("message", "registro não encontrado");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> conteudoLocais(Map<String, Object> detalhe) {
        return (List<Map<String, Object>>) detalhe.get("locations");
    }
}
