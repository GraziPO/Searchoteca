package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import searchoteca.model.DepartmentModel;
import searchoteca.repository.archive.DepartmentRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DepartmentController (/api/departamento): RF01 e RF02.
 * Também concentra o trio de segurança: sem token (401), sem permissão (403) e com permissão (200).
 */
class DepartmentControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/departamento";

    @Autowired
    private DepartmentRepository departmentRepository;

    private String tokenAcervo() {
        return tokenDoPerfil("warehouse_op");
    }

    private Map<String, Object> corpoDepartamento(String codigo, String nome) {
        return Map.of("departCode", codigo, "departName", nome, "departDesc", "Livros Aa - Ac");
    }

    // ----------------------------------------------------------------- POST /api/departamento

    @Test
    void create_semToken_deveRetornar401SemGravar() {
        // Arrange
        String codigo = "D" + uid();

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, null, corpoDepartamento(codigo, "Depto " + codigo));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(departmentRepository.findByDepartCode(codigo)).isNull();
    }

    @Test
    void create_comOperadorDeEmprestimo_deveRetornar403ComMensagemSemGravar() {
        // Arrange
        String codigo = "D" + uid();

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenDoPerfil("loan_op"), corpoDepartamento(codigo, "Depto " + codigo));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody())
                .containsEntry("status", 403)
                .containsEntry("message", MSG_SEM_PERMISSAO);
        assertThat(departmentRepository.findByDepartCode(codigo)).isNull();
    }

    @Test
    void create_comOperadorDeAcervo_deveRetornar200ComIdEGravarNoBanco() {
        // Arrange
        String codigo = "D" + uid();

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, tokenAcervo(), corpoDepartamento(codigo, "Depto " + codigo));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("departCode", codigo)
                .containsEntry("departName", "Depto " + codigo)
                .containsEntry("departDesc", "Livros Aa - Ac");
        assertThat(resposta.getBody().get("Id")).isNotNull();

        DepartmentModel gravado = departmentRepository.findByDepartCode(codigo);
        assertThat(gravado).isNotNull();
        assertThat(gravado.getId()).isEqualTo(comoLong(resposta.getBody().get("Id")));
    }

    @Test
    void create_comCodigoDuplicado_deveRetornar409ComMensagem() {
        // Arrange
        String codigo = (String) criarDepartamento().get("departCode");

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenAcervo(), corpoDepartamento(codigo, "Outro nome " + uid()));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody())
                .containsEntry("status", 409)
                .containsEntry("message", MSG_INTEGRIDADE);
    }

    // ----------------------------------------------------------------- GET /api/departamento

    @Test
    void getAll_deveRetornar200ComDepartamentoCriadoNaLista() {
        // Arrange
        String codigo = (String) criarDepartamento().get("departCode");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "departCode")).contains(codigo);
    }

    // ----------------------------------------------------------------- GET /api/departamento/{departCode}

    @Test
    void getById_comCodigoExistente_deveRetornar200ComTodosOsCampos() {
        // Arrange
        Map<String, Object> criado = criarDepartamento();
        String codigo = (String) criado.get("departCode");

        // Act - operador de empréstimo também pode consultar (org_depart:view)
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/" + codigo, tokenDoPerfil("loan_op"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("departCode", codigo)
                .containsEntry("departName", criado.get("departName"))
                .containsEntry("departDesc", criado.get("departDesc"))
                .containsEntry("Id", criado.get("Id"));
    }

    @Test
    void getById_comCodigoInexistente_deveRetornar404ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/NAOEXISTE" + uid(), tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody())
                .containsEntry("status", 404)
                .containsEntry("message", "Departamento não encontrado / não existente");
    }

    // ----------------------------------------------------------------- PUT /api/departamento/{departCode}

    @Test
    void update_comDadosValidos_deveRetornar200EGravarNovoNome() {
        // Arrange
        String codigo = (String) criarDepartamento().get("departCode");
        String novoNome = "Renomeado " + uid();

        // Act
        ResponseEntity<Map<String, Object>> resposta = put(BASE + "/" + codigo, tokenAcervo(),
                Map.of("departName", novoNome, "departDesc", "Livros Ad - Af"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("departCode", codigo)
                .containsEntry("departName", novoNome);
        assertThat(get(BASE + "/" + codigo, tokenAcervo()).getBody())         // efeito persistido
                .containsEntry("departName", novoNome)
                .containsEntry("departDesc", "Livros Ad - Af");
    }

    @Test
    void update_comCodigoInexistente_deveRetornar404ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = put(BASE + "/NAOEXISTE" + uid(), tokenAcervo(),
                Map.of("departName", "X", "departDesc", "Y"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("message", "Departamento inexistente");
    }

    // ----------------------------------------------------------------- DELETE /api/departamento/{departCode}

    @Test
    void delete_semVinculos_deveRetornar204EDepartamentoDeixaDeExistir() {
        // Arrange
        String codigo = (String) criarDepartamento().get("departCode");

        // Act
        ResponseEntity<Map<String, Object>> resposta = delete(BASE + "/" + codigo, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(BASE + "/" + codigo, tokenAcervo()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(departmentRepository.findByDepartCode(codigo)).isNull();
    }

    @Test
    void delete_comLocalizacaoVinculada_deveRetornar409EManterDepartamento() {
        // Arrange
        String codigo = (String) criarDepartamento().get("departCode");
        criarLocalizacao(codigo);

        // Act
        ResponseEntity<Map<String, Object>> resposta = delete(BASE + "/" + codigo, tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("message",
                "Este departamento não pode ser deletado, pois há localizações ou exemplares ligados a ele");
        assertThat(departmentRepository.findByDepartCode(codigo)).isNotNull();
    }

    // ----------------------------------------------------------------- GET /api/departamento/{departCode}/localizacoes

    @Test
    void getLocations_deveRetornarSomenteLocalizacoesDoDepartamento() {
        // Arrange
        String codigo = (String) criarDepartamento().get("departCode");
        String outroDepartamento = (String) criarDepartamento().get("departCode");
        String local1 = (String) criarLocalizacao(codigo).get("localCode");
        String local2 = (String) criarLocalizacao(codigo).get("localCode");
        String localDeOutro = (String) criarLocalizacao(outroDepartamento).get("localCode");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE + "/" + codigo + "/localizacoes", tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "localCode"))
                .containsExactlyInAnyOrder(local1, local2)
                .doesNotContain(localDeOutro);
        assertThat(valores(resposta.getBody(), "departCode")).containsOnly(codigo);
    }

    // ----------------------------------------------------------------- GET /api/departamento/{departCode}/livros

    @Test
    void getBooks_deveRetornarCadaLivroComExemplaresNoDepartamentoUmaUnicaVez() {
        // Arrange - 2 exemplares do livro A + 1 do livro B no mesmo departamento
        String codigo = (String) criarDepartamento().get("departCode");
        String local = (String) criarLocalizacao(codigo).get("localCode");
        String isbnA = (String) criarLivro().get("isbn");
        String isbnB = (String) criarLivro().get("isbn");
        criarExemplares(isbnA, codigo, local, 2);
        criarExemplares(isbnB, codigo, local, 1);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE + "/" + codigo + "/livros", tokenAcervo());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "isbn")).containsExactlyInAnyOrder(isbnA, isbnB);
        assertThat(valores(resposta.getBody(), "title")).doesNotContainNull();
    }
}
