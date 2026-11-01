package searchoteca.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base dos testes de integração: sobe a aplicação inteira em porta aleatória, com o
 * perfil "test" (H2 em memória), e faz chamadas HTTP reais com RestClient.
 *
 * Nada é mockado: controller, segurança, service, repository e banco são os reais.
 * Todos os dados são criados pelos próprios endpoints, com códigos únicos por execução.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String SENHA_PADRAO = "Senha@123";
    protected static final String MSG_SEM_PERMISSAO = "Você não tem permissão para esta ação.";
    protected static final String MSG_INTEGRIDADE =
            "Operação não permitida: o registro já existe ou está ligado a outros dados.";

    protected static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {};
    protected static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() {};

    private static final AtomicLong SEQUENCIA = new AtomicLong();

    // O contexto Spring (e o banco H2) é compartilhado entre as classes de teste,
    // então os tokens dos perfis também são: cada perfil faz login uma vez só.
    private static String tokenRoot;
    private static final Map<String, Usuario> USUARIOS_POR_PERFIL = new HashMap<>();

    @Value("${local.server.port}")
    private int port;

    @Value("${mfa.mock-code}")
    protected String codigoMfa;

    protected RestClient client;

    /** Usuário criado pela API; token fica nulo até o login. */
    protected record Usuario(Long id, String username, String email, String senha, String token) {}

    /** Aluno criado pela API (usuário com perfil "students" + registro em org_students). */
    protected record Aluno(String username, String senha, String ra) {}

    @BeforeEach
    void configurarCliente() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .requestFactory(new JdkClientHttpRequestFactory(                     // cliente do JDK: suporta PATCH
                        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                .defaultStatusHandler(status -> true, (req, res) -> {})      // 4xx/5xx não lançam exceção
                .build();

        if (tokenRoot == null) {
            // Usuário de bootstrap da própria carga inicial (permissions.sql / data-test.sql)
            tokenRoot = login("root", "root123");
        }
    }

    // =============================== identificadores únicos ===============================

    /** Sufixo único por chamada (base 36 do nanoTime + sequência). */
    protected static String uid() {
        return Long.toString(System.nanoTime() & Long.MAX_VALUE, 36) + SEQUENCIA.incrementAndGet();
    }

    /** ISBN de 13 dígitos, único na execução. */
    protected static String isbnUnico() {
        long n = Math.floorMod(System.nanoTime() + SEQUENCIA.incrementAndGet(), 100_000_000_000L);
        return String.format("97%011d", n);
    }

    protected static long comoLong(Object valor) {
        return ((Number) valor).longValue();
    }

    protected static List<Object> valores(List<Map<String, Object>> lista, String chave) {
        return lista.stream().map(item -> item.get(chave)).toList();
    }

    // =============================== chamadas HTTP ===============================

    private RestClient.RequestBodySpec requisicao(HttpMethod metodo, String uri, String token, Object corpo) {
        RestClient.RequestBodySpec spec = client.method(metodo).uri(uri).headers(h -> autorizar(h, token));
        if (corpo != null) {
            spec = spec.contentType(MediaType.APPLICATION_JSON).body(corpo);
        }
        return spec;
    }

    private static void autorizar(HttpHeaders headers, String token) {
        if (token != null) {
            headers.setBearerAuth(token);
        }
    }

    protected ResponseEntity<Map<String, Object>> get(String uri, String token) {
        return requisicao(HttpMethod.GET, uri, token, null).retrieve().toEntity(MAPA);
    }

    protected ResponseEntity<List<Map<String, Object>>> getLista(String uri, String token) {
        return requisicao(HttpMethod.GET, uri, token, null).retrieve().toEntity(LISTA);
    }

    protected ResponseEntity<Map<String, Object>> post(String uri, String token, Object corpo) {
        return requisicao(HttpMethod.POST, uri, token, corpo).retrieve().toEntity(MAPA);
    }

    protected ResponseEntity<List<Map<String, Object>>> postLista(String uri, String token, Object corpo) {
        return requisicao(HttpMethod.POST, uri, token, corpo).retrieve().toEntity(LISTA);
    }

    protected ResponseEntity<Map<String, Object>> put(String uri, String token, Object corpo) {
        return requisicao(HttpMethod.PUT, uri, token, corpo).retrieve().toEntity(MAPA);
    }

    protected ResponseEntity<Map<String, Object>> patch(String uri, String token) {
        return requisicao(HttpMethod.PATCH, uri, token, null).retrieve().toEntity(MAPA);
    }

    protected ResponseEntity<Map<String, Object>> delete(String uri, String token) {
        return requisicao(HttpMethod.DELETE, uri, token, null).retrieve().toEntity(MAPA);
    }

    // =============================== autenticação ===============================

    /** Login completo pelos endpoints reais: senha -> aceite dos termos (se pendente) -> código MFA. */
    protected String login(String username, String senha) {
        Map<String, Object> etapa = post("/api/auth/login", null, credenciais(username, senha)).getBody();
        assertThat(etapa).as("resposta do login de %s", username).isNotNull();

        if (Boolean.TRUE.equals(etapa.get("termsRequired"))) {
            etapa = post("/api/auth/terms/accept", null, Map.of(
                    "termsToken", etapa.get("termsToken"),
                    "termsVersion", etapa.get("termsVersion"),
                    "privacyVersion", etapa.get("privacyVersion"))).getBody();
            assertThat(etapa).as("aceite dos termos de %s", username).isNotNull();
        }
        if (Boolean.TRUE.equals(etapa.get("mfaRequired"))) {
            etapa = post("/api/auth/mfa/verify", null, Map.of(
                    "mfaToken", etapa.get("mfaToken"),
                    "code", codigoMfa)).getBody();
            assertThat(etapa).as("verificação MFA de %s", username).isNotNull();
        }
        assertThat(etapa.get("token")).as("JWT de %s", username).isNotNull();
        return (String) etapa.get("token");
    }

    protected static Map<String, Object> credenciais(String username, String senha) {
        return Map.of("username", username, "password", senha);
    }

    protected String tokenRoot() {
        return tokenRoot;
    }

    /** Token de um usuário do perfil informado, criado e logado uma única vez por execução. */
    protected String tokenDoPerfil(String perfil) {
        Usuario usuario = USUARIOS_POR_PERFIL.get(perfil);
        if (usuario == null) {
            usuario = usuarioLogado(perfil);
            USUARIOS_POR_PERFIL.put(perfil, usuario);
        }
        return usuario.token();
    }

    protected Usuario usuarioDoPerfil(String perfil) {
        tokenDoPerfil(perfil);
        return USUARIOS_POR_PERFIL.get(perfil);
    }

    // =============================== criação de dados pela API ===============================

    protected Map<String, Object> corpoUsuario(String username, String email, String perfil) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("username", username);
        corpo.put("completeName", "Usuário " + username);
        corpo.put("email", email);
        corpo.put("password", SENHA_PADRAO);
        corpo.put("role_code", perfil);
        return corpo;
    }

    /** Cria (pelo root) um usuário novo do perfil informado, sem fazer login. */
    protected Usuario criarUsuario(String perfil) {
        String id = uid();
        String username = "u" + id;
        String email = "u" + id + "@teste.br";

        ResponseEntity<Map<String, Object>> resposta =
                post("/api/usuarios", tokenRoot(), corpoUsuario(username, email, perfil));

        assertThat(resposta.getStatusCode()).as("criação do usuário %s", username).isEqualTo(HttpStatus.OK);
        return new Usuario(comoLong(resposta.getBody().get("userId")), username, email, SENHA_PADRAO, null);
    }

    protected Usuario usuarioLogado(String perfil) {
        Usuario u = criarUsuario(perfil);
        return new Usuario(u.id(), u.username(), u.email(), u.senha(), login(u.username(), u.senha()));
    }

    protected Map<String, Object> criarDepartamento() {
        String id = uid();
        ResponseEntity<Map<String, Object>> resposta = post("/api/departamento", tokenRoot(), Map.of(
                "departCode", "D" + id,
                "departName", "Depto " + id,
                "departDesc", "Departamento de teste"));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    protected Map<String, Object> criarLocalizacao(String departCode) {
        String id = uid();
        ResponseEntity<Map<String, Object>> resposta = post("/api/localizacao", tokenRoot(), Map.of(
                "localCode", "L" + id,
                "localName", "Local " + id,
                "localDesc", "Prateleira de teste",
                "departCode", departCode));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    protected Map<String, Object> corpoLivro(String isbn, String titulo) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("isbn", isbn);
        corpo.put("title", titulo);
        corpo.put("author", "Autor de Teste");
        corpo.put("rel_year", 2020);
        corpo.put("publisher", "Editora Teste");
        corpo.put("genre", "Romance");
        return corpo;
    }

    protected Map<String, Object> criarLivro(String titulo) {
        ResponseEntity<Map<String, Object>> resposta = post("/api/livro", tokenRoot(), corpoLivro(isbnUnico(), titulo));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    protected Map<String, Object> criarLivro() {
        return criarLivro("Livro " + uid());
    }

    protected Map<String, Object> corpoExemplares(String isbn, String departCode, String localCode, int quantidade) {
        return Map.of("isbn", isbn, "departCode", departCode, "localCode", localCode, "quantity", quantidade);
    }

    protected List<Map<String, Object>> criarExemplares(String isbn, String departCode, String localCode, int quantidade) {
        ResponseEntity<List<Map<String, Object>>> resposta =
                postLista("/api/exemplares", tokenRoot(), corpoExemplares(isbn, departCode, localCode, quantidade));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(quantidade);
        return resposta.getBody();
    }

    /** Monta departamento + localização + livro + exemplares; devolve os ids dos exemplares. */
    protected List<Long> criarExemplaresDeUmLivroNovo(int quantidade) {
        String departCode = (String) criarDepartamento().get("departCode");
        String localCode = (String) criarLocalizacao(departCode).get("localCode");
        String isbn = (String) criarLivro().get("isbn");
        return criarExemplares(isbn, departCode, localCode, quantidade).stream()
                .map(etiqueta -> comoLong(etiqueta.get("copyId")))
                .toList();
    }

    protected Map<String, Object> corpoAluno(String username, String ra) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("username", username);
        corpo.put("completeName", "Aluno " + username);
        corpo.put("email", username + "@aluno.br");
        corpo.put("password", SENHA_PADRAO);
        corpo.put("ra", ra);
        corpo.put("phone", "11999990000");
        return corpo;
    }

    protected Aluno criarAluno() {
        String id = uid();
        String username = "a" + id;
        String ra = "RA" + id;
        ResponseEntity<Map<String, Object>> resposta = post("/api/usuarios/aluno", tokenRoot(), corpoAluno(username, ra));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return new Aluno(username, SENHA_PADRAO, ra);
    }
}
