package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditTrailController (/api/auditTrail): trilha de auditoria, gravada pelas outras rotas.
 */
class AuditTrailControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/auditTrail";

    /** Operador novo faz login e cria um departamento: ações que devem ir para a auditoria. */
    private Usuario operadorQueCriouDepartamento(String departCode) {
        Usuario operador = usuarioLogado("warehouse_op");
        ResponseEntity<Map<String, Object>> criacao = post("/api/departamento", operador.token(), Map.of(
                "departCode", departCode, "departName", "Depto " + departCode, "departDesc", "Auditoria"));
        assertThat(criacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        return operador;
    }

    @Test
    void list_comRootFiltrandoPorUsuario_deveRetornar200ComAcoesGravadasPelasOutrasRotas() {
        // Arrange
        String departCode = "D" + uid();
        Usuario operador = operadorQueCriouDepartamento(departCode);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta =
                getLista(BASE + "?usuario=" + operador.username(), tokenRoot());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> registros = resposta.getBody();
        assertThat(valores(registros, "username")).containsOnly(operador.username());
        assertThat(valores(registros, "action")).contains("TERMS_ACCEPTED", "LOGIN_SUCCESS", "CREATE");
        assertThat(registros).anySatisfy(registro -> assertThat(registro)
                .containsEntry("action", "CREATE")
                .containsEntry("details", "department" + departCode));
    }

    @Test
    void list_comLimite1_deveRetornarSomenteORegistroMaisRecente() {
        // Arrange
        String departCode = "D" + uid();
        Usuario operador = operadorQueCriouDepartamento(departCode);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta =
                getLista(BASE + "?usuario=" + operador.username() + "&limite=1", tokenRoot());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0))
                .containsEntry("action", "CREATE")
                .containsEntry("details", "department" + departCode);
    }

    @Test
    void list_comOperadorDeAcervo_deveRetornar403ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE, tokenDoPerfil("warehouse_op"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody())
                .containsEntry("status", 403)
                .containsEntry("message", MSG_SEM_PERMISSAO);
    }

    @Test
    void list_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE, null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).isNull();
    }
}
