package searchoteca.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import searchoteca.repository.catalog.CopyRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CopyController (/api/exemplares): RF06 - exemplares com código único
 * (ISBN + departamento + localização + índice) e reimpressão de etiquetas.
 */
class CopyControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/exemplares";

    @Autowired
    private CopyRepository copyRepository;

    private String departCode;
    private String localCode;
    private String isbn;
    private String titulo;

    @BeforeEach
    void prepararAcervo() {
        departCode = (String) criarDepartamento().get("departCode");
        localCode = (String) criarLocalizacao(departCode).get("localCode");
        Map<String, Object> livro = criarLivro();
        isbn = (String) livro.get("isbn");
        titulo = (String) livro.get("title");
    }

    private String tokenAcervo() {
        return tokenDoPerfil("warehouse_op");
    }

    private String codigoEsperado(int indice) {
        return isbn + departCode + localCode + indice;
    }

    // ----------------------------------------------------------------- POST /api/exemplares

    @Test
    void createCopy_comQuantidadeTres_deveRetornar200ComTresEtiquetasEGravarNoBanco() {
        // Act
        ResponseEntity<List<Map<String, Object>>> resposta =
                postLista(BASE, tokenAcervo(), corpoExemplares(isbn, departCode, localCode, 3));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(3);
        assertThat(valores(resposta.getBody(), "code"))
                .containsExactly(codigoEsperado(1), codigoEsperado(2), codigoEsperado(3));
        assertThat(valores(resposta.getBody(), "title")).containsOnly(titulo);
        assertThat(valores(resposta.getBody(), "localCode")).containsOnly(localCode);
        assertThat(valores(resposta.getBody(), "copyId")).doesNotContainNull();
        assertThat(copyRepository.countByIsbn(isbn)).isEqualTo(3L);
    }

    @Test
    void createCopy_comExemplaresJaExistentes_deveContinuarANumeracao() {
        // Arrange
        criarExemplares(isbn, departCode, localCode, 2);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta =
                postLista(BASE, tokenAcervo(), corpoExemplares(isbn, departCode, localCode, 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "code")).containsExactly(codigoEsperado(3));
        assertThat(copyRepository.countByIsbn(isbn)).isEqualTo(3L);
    }

    @Test
    void createCopy_comQuantidadeZero_deveRetornar409ComMensagemSemGravar() {
        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenAcervo(), corpoExemplares(isbn, departCode, localCode, 0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody())
                .containsEntry("status", 409)
                .containsEntry("message", "A quantidade deve estar entre 1 e 100");
        assertThat(copyRepository.countByIsbn(isbn)).isZero();
    }

    @Test
    void createCopy_comLivroInexistente_deveRetornar404ComMensagem() {
        // Arrange
        String isbnInexistente = isbnUnico();

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenAcervo(), corpoExemplares(isbnInexistente, departCode, localCode, 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody())
                .containsEntry("status", 404)
                .containsEntry("message", "Este livro não existe");
    }

    @Test
    void createCopy_comLocalizacaoDeOutroDepartamento_deveRetornar409ComMensagem() {
        // Arrange
        String outroDepartamento = (String) criarDepartamento().get("departCode");

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenAcervo(), corpoExemplares(isbn, outroDepartamento, localCode, 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("message", "Esta localização não pertence ao departamento");
        assertThat(copyRepository.countByIsbn(isbn)).isZero();
    }

    @Test
    void createCopy_comOperadorDeEmprestimo_deveRetornar403ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenDoPerfil("loan_op"), corpoExemplares(isbn, departCode, localCode, 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("message", MSG_SEM_PERMISSAO);
        assertThat(copyRepository.countByIsbn(isbn)).isZero();
    }

    // ----------------------------------------------------------------- GET /api/exemplares/etiquetas

    @Test
    void printLabels_comIdsExistentes_deveRetornar200ComEtiquetasDoLivro() {
        // Arrange
        List<Map<String, Object>> etiquetas = criarExemplares(isbn, departCode, localCode, 2);
        Object id1 = etiquetas.get(0).get("copyId");
        Object id2 = etiquetas.get(1).get("copyId");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta =
                getLista(BASE + "/etiquetas?labels=" + id1 + "&labels=" + id2, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "copyId")).containsExactly(id1, id2);
        assertThat(valores(resposta.getBody(), "code")).containsExactly(codigoEsperado(1), codigoEsperado(2));
        assertThat(valores(resposta.getBody(), "author")).containsOnly("Autor de Teste");
    }

    @Test
    void printLabels_comIdInexistente_deveRetornar404ComIdFaltanteNaMensagem() {
        // Arrange
        Object idExistente = criarExemplares(isbn, departCode, localCode, 1).get(0).get("copyId");
        long idInexistente = 999_999_999L;

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                get(BASE + "/etiquetas?labels=" + idExistente + "&labels=" + idInexistente, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((String) resposta.getBody().get("message"))
                .startsWith("exemplares não encontrados")
                .contains(String.valueOf(idInexistente));
    }
}
