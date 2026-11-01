package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import searchoteca.model.BookModel;
import searchoteca.repository.catalog.BookRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BookController (/api/livro): RF05 - registro-base do livro pelo ISBN.
 */
class BookControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/livro";

    @Autowired
    private BookRepository bookRepository;

    private String tokenAcervo() {
        return tokenDoPerfil("warehouse_op");
    }

    // ----------------------------------------------------------------- POST /api/livro

    @Test
    void create_comDadosValidos_deveRetornar200ComIdEGravarNoBanco() {
        // Arrange
        String isbn = isbnUnico();

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, tokenAcervo(), corpoLivro(isbn, "Dom Casmurro"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("isbn", isbn)
                .containsEntry("title", "Dom Casmurro")
                .containsEntry("author", "Autor de Teste")
                .containsEntry("rel_year", 2020);
        assertThat(resposta.getBody().get("Id")).isNotNull();

        BookModel gravado = bookRepository.findByIsbn(isbn);
        assertThat(gravado).isNotNull();
        assertThat(gravado.getTitle()).isEqualTo("Dom Casmurro");
    }

    @Test
    void create_comIsbnDuplicado_deveRetornar409ComMensagem() {
        // Arrange
        String isbn = (String) criarLivro().get("isbn");

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, tokenAcervo(), corpoLivro(isbn, "Duplicado"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody())
                .containsEntry("status", 409)
                .containsEntry("message", MSG_INTEGRIDADE);
        assertThat(bookRepository.findByIsbn(isbn).getTitle()).isNotEqualTo("Duplicado");
    }

    @Test
    void create_comOperadorDeEmprestimo_deveRetornar403SemGravar() {
        // Arrange
        String isbn = isbnUnico();

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, tokenDoPerfil("loan_op"), corpoLivro(isbn, "Bloqueado"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("message", MSG_SEM_PERMISSAO);
        assertThat(bookRepository.findByIsbn(isbn)).isNull();
    }

    // ----------------------------------------------------------------- GET /api/livro

    @Test
    void getAll_comUsuarioAutenticado_deveRetornar200ComLivroCriado() {
        // Arrange
        String isbn = (String) criarLivro().get("isbn");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE, tokenDoPerfil("loan_op"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "isbn")).contains(isbn);
    }

    @Test
    void getAll_semToken_deveRetornar401() {
        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE, null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).isNull();
    }

    // ----------------------------------------------------------------- GET /api/livro/{isbn}

    @Test
    void getByIsbn_comIsbnExistente_deveRetornar200ComTodosOsCampos() {
        // Arrange
        Map<String, Object> criado = criarLivro("Memórias Póstumas " + uid());
        String isbn = (String) criado.get("isbn");

        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/" + isbn, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("Id", criado.get("Id"))
                .containsEntry("isbn", isbn)
                .containsEntry("title", criado.get("title"))
                .containsEntry("author", "Autor de Teste")
                .containsEntry("rel_year", 2020)
                .containsEntry("publisher", "Editora Teste")
                .containsEntry("genre", "Romance");
    }

    @Test
    void getByIsbn_comIsbnInexistente_deveRetornar404SemCorpo() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/" + isbnUnico(), tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).isNull();
    }

    // ----------------------------------------------------------------- PUT /api/livro/{isbn}

    @Test
    void update_comDadosValidos_deveRetornar200EGravarNovoTitulo() {
        // Arrange
        String isbn = (String) criarLivro().get("isbn");
        Map<String, Object> novosDados = corpoLivro(isbn, "Título Revisado");
        novosDados.put("rel_year", null);               // ano de lançamento é opcional

        // Act
        ResponseEntity<Map<String, Object>> resposta = put(BASE + "/" + isbn, tokenAcervo(), novosDados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> consultado = get(BASE + "/" + isbn, tokenAcervo()).getBody();   // efeito persistido
        assertThat(consultado)
                .containsEntry("title", "Título Revisado")
                .containsEntry("isbn", isbn);
        assertThat(consultado.get("rel_year")).isNull();
    }

    @Test
    void update_comIsbnInexistente_deveRetornar404ComMensagem() {
        // Arrange
        String isbn = isbnUnico();

        // Act
        ResponseEntity<Map<String, Object>> resposta = put(BASE + "/" + isbn, tokenAcervo(), corpoLivro(isbn, "X"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody())
                .containsEntry("status", 404)
                .containsEntry("message", "registro não encontrado");
    }

    // ----------------------------------------------------------------- DELETE /api/livro/{isbn}

    @Test
    void delete_semExemplares_deveRetornar204ELivroDeixaDeExistir() {
        // Arrange
        String isbn = (String) criarLivro().get("isbn");

        // Act
        ResponseEntity<Map<String, Object>> resposta = delete(BASE + "/" + isbn, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(BASE + "/" + isbn, tokenAcervo()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bookRepository.findByIsbn(isbn)).isNull();
    }

    @Test
    void delete_comExemplaresCadastrados_deveRetornar409EManterLivro() {
        // Arrange
        String departCode = (String) criarDepartamento().get("departCode");
        String localCode = (String) criarLocalizacao(departCode).get("localCode");
        String isbn = (String) criarLivro().get("isbn");
        criarExemplares(isbn, departCode, localCode, 1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = delete(BASE + "/" + isbn, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("message",
                "Não é possível excluir: este livro tem exemplares cadastrados.");
        assertThat(bookRepository.findByIsbn(isbn)).isNotNull();
    }

    // ----------------------------------------------------------------- GET /api/livro/{isbn}/locais

    @Test
    void findLocations_deveRetornarCadaLocalizacaoOndeHaExemplaresDoLivro() {
        // Arrange - exemplares do mesmo livro em duas localizações (uma delas com 2 exemplares)
        String departCode = (String) criarDepartamento().get("departCode");
        String local1 = (String) criarLocalizacao(departCode).get("localCode");
        String local2 = (String) criarLocalizacao(departCode).get("localCode");
        String isbn = (String) criarLivro().get("isbn");
        criarExemplares(isbn, departCode, local1, 2);
        criarExemplares(isbn, departCode, local2, 1);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE + "/" + isbn + "/locais", tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).doesNotContainNull();
        assertThat(valores(resposta.getBody(), "localCode")).containsExactlyInAnyOrder(local1, local2);
        assertThat(valores(resposta.getBody(), "departCode")).containsOnly(departCode);
    }
}
