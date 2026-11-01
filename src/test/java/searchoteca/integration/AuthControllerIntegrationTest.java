package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import searchoteca.legal.TermsAcceptanceRepository;
import searchoteca.mfa.CodeRepository;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthController (/api/auth): login em três etapas (senha -> aceite dos termos -> código MFA).
 * O MFA roda em modo de teste (mfa.mock-code): nenhum e-mail é enviado, o resto do fluxo é real.
 */
class AuthControllerIntegrationTest extends IntegrationTestBase {

    private static final String LOGIN = "/api/auth/login";
    private static final String ACEITE = "/api/auth/terms/accept";
    private static final String VERIFICAR = "/api/auth/mfa/verify";
    private static final String REENVIAR = "/api/auth/mfa/resend";

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private TermsAcceptanceRepository termsAcceptanceRepository;

    // ----------------------------------------------------------------- helpers

    private Map<String, Object> iniciarLogin(Usuario usuario) {
        return post(LOGIN, null, credenciais(usuario.username(), usuario.senha())).getBody();
    }

    private Map<String, Object> corpoAceite(Map<String, Object> desafioTermos, String versaoTermos) {
        return Map.of(
                "termsToken", desafioTermos.get("termsToken"),
                "termsVersion", versaoTermos,
                "privacyVersion", desafioTermos.get("privacyVersion"));
    }

    /** Login até o desafio de MFA (senha + aceite); devolve o corpo com o mfaToken. */
    private Map<String, Object> desafioMfa(Usuario usuario) {
        Map<String, Object> termos = iniciarLogin(usuario);
        return post(ACEITE, null, corpoAceite(termos, (String) termos.get("termsVersion"))).getBody();
    }

    private long codigosGerados(Usuario usuario) {
        return codeRepository.countCreatedSince(usuario.id(), LocalDateTime.now().minusHours(1));
    }

    private String codigoErrado() {
        return "000000".equals(codigoMfa) ? "111111" : "000000";
    }

    // ----------------------------------------------------------------- POST /api/auth/login

    @Test
    void login_usuarioSemAceiteDosTermos_deveRetornar200ComTermsTokenSemGerarCodigoMfa() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(LOGIN, null, credenciais(usuario.username(), usuario.senha()));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("termsRequired", true)
                .containsEntry("termsVersion", "1.0")
                .containsEntry("privacyVersion", "1.0")
                .doesNotContainKey("token");
        assertThat((String) resposta.getBody().get("termsToken")).isNotBlank();
        assertThat(codigosGerados(usuario)).isZero();      // sem aceite, nenhum código MFA é emitido
    }

    @Test
    void login_comTermosJaAceitos_devePularAceiteEIrDiretoParaMfa() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");
        login(usuario.username(), usuario.senha());          // primeiro acesso grava o aceite

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(LOGIN, null, credenciais(usuario.username(), usuario.senha()));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("mfaRequired", true)
                .doesNotContainKey("termsRequired");
        assertThat((String) resposta.getBody().get("mfaToken")).isNotBlank();
    }

    @Test
    void login_comSenhaErrada_deveRetornar401ComMensagem() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(LOGIN, null, credenciais(usuario.username(), "senha-errada"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody())
                .containsEntry("status", 401)
                .containsEntry("message", "Usuário ou senha inválidos");
    }

    @Test
    void login_comUsuarioDesativado_deveRetornar401ComMensagem() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");
        patch("/api/usuarios/" + usuario.username() + "/status/desativar", tokenRoot());

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(LOGIN, null, credenciais(usuario.username(), usuario.senha()));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("message", "Usuário ou senha inválidos");
    }

    @Test
    void login_deveResponderJsonComHeadersDeSegurancaESemCache() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(LOGIN, null, credenciais(usuario.username(), usuario.senha()));

        // Assert - a resposta carrega um token: não pode ficar em cache nem ser interpretada como outro tipo
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getContentType()).isNotNull();
        assertThat(resposta.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
        assertThat(resposta.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(resposta.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
    }

    // ----------------------------------------------------------------- POST /api/auth/terms/accept

    @Test
    void acceptTerms_comVersoesAtuais_deveRetornarDesafioMfaEGravarAceite() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");
        Map<String, Object> termos = iniciarLogin(usuario);

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(ACEITE, null, corpoAceite(termos, "1.0"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> corpo = resposta.getBody();
        assertThat(corpo).containsEntry("mfaRequired", true);
        assertThat((String) corpo.get("mfaToken")).isNotBlank();
        assertThat(comoLong(corpo.get("expiresInSeconds"))).isEqualTo(600L);
        assertThat((String) corpo.get("email"))                // e-mail mascarado (LGPD)
                .endsWith("@teste.br")
                .contains("*")
                .isNotEqualTo(usuario.email());
        assertThat(termsAcceptanceRepository.countAccepted(usuario.id(), "1.0", "1.0")).isEqualTo(1L);
        assertThat(codigosGerados(usuario)).isEqualTo(1L);
    }

    @Test
    void acceptTerms_comVersaoDesatualizada_deveRetornar409ComMensagemSemGravarAceite() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");
        Map<String, Object> termos = iniciarLogin(usuario);

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(ACEITE, null, corpoAceite(termos, "0.9"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("message",
                "Os documentos foram atualizados. Leia a versão atual e aceite novamente.");
        assertThat(termsAcceptanceRepository.countAccepted(usuario.id(), "1.0", "1.0")).isZero();
    }

    @Test
    void acceptTerms_comTokenInvalido_deveRetornar401ComMensagem() {
        // Arrange
        Map<String, Object> corpo = Map.of("termsToken", "token-invalido", "termsVersion", "1.0", "privacyVersion", "1.0");

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(ACEITE, null, corpo);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("message",
                "Sessão de aceite expirada ou inválida. Faça login novamente.");
    }

    // ----------------------------------------------------------------- POST /api/auth/mfa/verify

    @Test
    void verifyMfa_comCodigoCorreto_deveRetornar200ComJwtQueAcessaRotaProtegida() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");
        String mfaToken = (String) desafioMfa(usuario).get("mfaToken");

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(VERIFICAR, null, Map.of("mfaToken", mfaToken, "code", codigoMfa));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("username", usuario.username())
                .containsEntry("role_code", "warehouse_op");
        String jwt = (String) resposta.getBody().get("token");
        assertThat(jwt).isNotBlank();
        assertThat(getLista("/api/departamento", jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void verifyMfa_comCodigoIncorreto_deveRetornar401ComTentativasRestantes() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");
        String mfaToken = (String) desafioMfa(usuario).get("mfaToken");

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(VERIFICAR, null, Map.of("mfaToken", mfaToken, "code", codigoErrado()));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("message", "Código incorreto. Restam 4 tentativa(s).");
    }

    @Test
    void verifyMfa_comCodigoJaUtilizado_deveRetornar401ComMensagem() {
        // Arrange - o código é de uso único: a primeira verificação o marca como usado no banco
        Usuario usuario = criarUsuario("warehouse_op");
        String mfaToken = (String) desafioMfa(usuario).get("mfaToken");
        Map<String, Object> corpo = Map.of("mfaToken", mfaToken, "code", codigoMfa);
        post(VERIFICAR, null, corpo);

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(VERIFICAR, null, corpo);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("message",
                "Código expirado ou já utilizado. Solicite um novo código.");
    }

    @Test
    void rotaProtegida_comMfaTokenNoLugarDoJwt_deveRetornar401() {
        // Arrange - o mfaToken é assinado pela aplicação, mas só serve para concluir o login
        Usuario usuario = criarUsuario("warehouse_op");
        String mfaToken = (String) desafioMfa(usuario).get("mfaToken");

        // Act
        ResponseEntity<Map<String, Object>> resposta = get("/api/departamento", mfaToken);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).isNull();
    }

    // ----------------------------------------------------------------- POST /api/auth/mfa/resend

    @Test
    void resendMfa_comTokenValido_deveRetornar200EGerarNovoCodigo() {
        // Arrange
        Usuario usuario = criarUsuario("warehouse_op");
        String mfaToken = (String) desafioMfa(usuario).get("mfaToken");

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(REENVIAR, null, Map.of("mfaToken", mfaToken));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("mfaRequired", true)
                .containsEntry("mfaToken", mfaToken);
        assertThat(codigosGerados(usuario)).isEqualTo(2L);
        assertThat(post(VERIFICAR, null, Map.of("mfaToken", mfaToken, "code", codigoMfa)).getStatusCode())
                .isEqualTo(HttpStatus.OK);                  // o novo código é o que vale
    }

    @Test
    void resendMfa_comTokenInvalido_deveRetornar401ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = post(REENVIAR, null, Map.of("mfaToken", "token-invalido"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("message",
                "Verificação expirada ou inválida. Faça login novamente.");
    }
}
