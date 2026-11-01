package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LegalController (/api/legal): documentos públicos, lidos antes do login.
 */
class LegalControllerIntegrationTest extends IntegrationTestBase {

    private static String conteudoDoArquivo(String caminho) throws IOException {
        return new ClassPathResource(caminho).getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void termos_semToken_deveRetornar200ComDocumentoNaVersaoAtual() throws IOException {
        // Arrange
        String esperado = conteudoDoArquivo("legal/termos-de-uso.md");

        // Act
        ResponseEntity<Map<String, Object>> resposta = get("/api/legal/termos", null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("title", "Termos de Uso")
                .containsEntry("version", "1.0")
                .containsEntry("content", esperado);
    }

    @Test
    void privacidade_semToken_deveRetornar200ComDocumentoNaVersaoAtual() throws IOException {
        // Arrange
        String esperado = conteudoDoArquivo("legal/politica-de-privacidade.md");

        // Act
        ResponseEntity<Map<String, Object>> resposta = get("/api/legal/privacidade", null);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("title", "Política de Privacidade")
                .containsEntry("version", "1.0")
                .containsEntry("content", esperado);
    }
}
