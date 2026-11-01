package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import searchoteca.model.LocationModel;
import searchoteca.repository.archive.LocationRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LocationController (/api/localizacao): RF03 e RF04.
 */
class LocationControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/localizacao";

    @Autowired
    private LocationRepository locationRepository;

    private String tokenAcervo() {
        return tokenDoPerfil("warehouse_op");
    }

    private Map<String, Object> corpoLocalizacao(String codigo, String departCode) {
        return Map.of(
                "localCode", codigo,
                "localName", "Local " + codigo,
                "localDesc", "Prateleira A",
                "departCode", departCode);
    }

    // ----------------------------------------------------------------- POST /api/localizacao

    @Test
    void create_comOperadorDeAcervo_deveRetornar200ComIdEGravarNoBanco() {
        // Arrange
        String departCode = (String) criarDepartamento().get("departCode");
        String codigo = "L" + uid();

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, tokenAcervo(), corpoLocalizacao(codigo, departCode));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("localCode", codigo)
                .containsEntry("localName", "Local " + codigo)
                .containsEntry("departCode", departCode);
        assertThat(resposta.getBody().get("id")).isNotNull();

        LocationModel gravada = locationRepository.findByLocalCode(codigo);
        assertThat(gravada).isNotNull();
        assertThat(gravada.getDepartCode()).isEqualTo(departCode);
    }

    @Test
    void create_comCodigoDuplicado_deveRetornar409ComMensagem() {
        // Arrange
        String departCode = (String) criarDepartamento().get("departCode");
        String codigo = (String) criarLocalizacao(departCode).get("localCode");
        Map<String, Object> corpo = Map.of(
                "localCode", codigo, "localName", "Outro nome " + uid(),
                "localDesc", "Prateleira B", "departCode", departCode);

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, tokenAcervo(), corpo);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody())
                .containsEntry("status", 409)
                .containsEntry("message", "Já existe uma localização com esse código.");
    }

    @Test
    void create_comDepartamentoInexistente_deveRetornar409SemGravar() {
        // Arrange
        String codigo = "L" + uid();

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenAcervo(), corpoLocalizacao(codigo, "X" + uid()));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("message", MSG_INTEGRIDADE);
        assertThat(locationRepository.findByLocalCode(codigo)).isNull();
    }

    @Test
    void create_comOperadorDeEmprestimo_deveRetornar403ComMensagem() {
        // Arrange
        String departCode = (String) criarDepartamento().get("departCode");

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenDoPerfil("loan_op"), corpoLocalizacao("L" + uid(), departCode));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("message", MSG_SEM_PERMISSAO);
    }

    // ----------------------------------------------------------------- GET /api/localizacao

    @Test
    void getAll_comOperadorDeEmprestimo_deveRetornar200ComLocalizacaoCriada() {
        // Arrange
        String departCode = (String) criarDepartamento().get("departCode");
        String codigo = (String) criarLocalizacao(departCode).get("localCode");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE, tokenDoPerfil("loan_op"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "localCode")).contains(codigo);
    }

    // ----------------------------------------------------------------- GET /api/localizacao/{localCode}

    @Test
    void getById_comCodigoExistente_deveRetornar200ComTodosOsCampos() {
        // Arrange
        String departCode = (String) criarDepartamento().get("departCode");
        Map<String, Object> criada = criarLocalizacao(departCode);
        String codigo = (String) criada.get("localCode");

        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/" + codigo, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("id", criada.get("id"))
                .containsEntry("localCode", codigo)
                .containsEntry("localName", criada.get("localName"))
                .containsEntry("localDesc", criada.get("localDesc"))
                .containsEntry("departCode", departCode);
    }

    @Test
    void getById_comCodigoInexistente_deveRetornar404ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/NAOEXISTE" + uid(), tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody())
                .containsEntry("status", 404)
                .containsEntry("message", "Localização não encontrada");
    }

    // ----------------------------------------------------------------- PUT /api/localizacao/{localCode}

    @Test
    void update_paraOutroDepartamento_deveRetornar200EMudarVinculo() {
        // Arrange
        String departOrigem = (String) criarDepartamento().get("departCode");
        String departDestino = (String) criarDepartamento().get("departCode");
        String codigo = (String) criarLocalizacao(departOrigem).get("localCode");
        String novoNome = "Movida " + uid();

        // Act
        ResponseEntity<Map<String, Object>> resposta = put(BASE + "/" + codigo, tokenAcervo(),
                Map.of("localName", novoNome, "localDesc", "Prateleira C", "departCode", departDestino));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("localCode", codigo)
                .containsEntry("localName", novoNome)
                .containsEntry("departCode", departDestino);
        assertThat(locationRepository.findByLocalCode(codigo).getDepartCode()).isEqualTo(departDestino);
        assertThat(valores(getLista("/api/departamento/" + departDestino + "/localizacoes", tokenAcervo()).getBody(),
                "localCode")).contains(codigo);
        assertThat(valores(getLista("/api/departamento/" + departOrigem + "/localizacoes", tokenAcervo()).getBody(),
                "localCode")).doesNotContain(codigo);
    }

    @Test
    void update_comCodigoInexistente_deveRetornar404ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = put(BASE + "/NAOEXISTE" + uid(), tokenAcervo(),
                Map.of("localName", "X", "localDesc", "Y"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("message", "Registro inexistente");
    }

    // ----------------------------------------------------------------- DELETE /api/localizacao/{localCode}

    @Test
    void delete_semExemplares_deveRetornar204ELocalizacaoDeixaDeExistir() {
        // Arrange
        String departCode = (String) criarDepartamento().get("departCode");
        String codigo = (String) criarLocalizacao(departCode).get("localCode");

        // Act
        ResponseEntity<Map<String, Object>> resposta = delete(BASE + "/" + codigo, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(BASE + "/" + codigo, tokenAcervo()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(locationRepository.existsByLocalCode(codigo)).isFalse();
    }

    @Test
    void delete_comExemplaresGuardados_deveRetornar409EManterLocalizacao() {
        // Arrange
        String departCode = (String) criarDepartamento().get("departCode");
        String codigo = (String) criarLocalizacao(departCode).get("localCode");
        criarExemplares((String) criarLivro().get("isbn"), departCode, codigo, 1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = delete(BASE + "/" + codigo, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("message",
                "Não é possível excluir: há exemplares guardados nesta localização.");
        assertThat(locationRepository.existsByLocalCode(codigo)).isTrue();
    }

    // ----------------------------------------------------------------- GET /api/localizacao/{localCode}/livros

    @Test
    void getBooks_deveRetornarCadaLivroUmaUnicaVezMesmoComVariosExemplares() {
        // Arrange - 3 exemplares do livro A + 1 do livro B na mesma localização
        String departCode = (String) criarDepartamento().get("departCode");
        String codigo = (String) criarLocalizacao(departCode).get("localCode");
        String isbnA = (String) criarLivro().get("isbn");
        String isbnB = (String) criarLivro().get("isbn");
        criarExemplares(isbnA, departCode, codigo, 3);
        criarExemplares(isbnB, departCode, codigo, 1);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE + "/" + codigo + "/livros", tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "isbn")).containsExactlyInAnyOrder(isbnA, isbnB);
    }
}
