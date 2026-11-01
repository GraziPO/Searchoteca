package searchoteca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import searchoteca.model.StudentModel;
import searchoteca.model.UserModel;
import searchoteca.repository.users.StudentRepository;
import searchoteca.repository.users.UserRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserController (/api/usuarios): cadastro e manutenção de usuários internos e de alunos.
 */
class UserControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE = "/api/usuarios";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    private String tokenAdmin() {
        return tokenDoPerfil("admin_users");
    }

    // ----------------------------------------------------------------- GET /api/usuarios

    @Test
    void findAll_comAdminDeUsuarios_deveRetornar200ComUsuarioCriadoNaLista() {
        // Arrange
        Usuario novo = criarUsuario("loan_op");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE, tokenAdmin());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valores(resposta.getBody(), "username")).contains(novo.username(), "root");
    }

    @Test
    void findAll_naoDeveExporHashDaSenhaDosUsuarios() {
        // Arrange
        criarUsuario("loan_op");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = getLista(BASE, tokenAdmin());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .isNotEmpty()
                .allSatisfy(usuario -> assertThat(usuario).doesNotContainKey("password"));
    }

    @Test
    void findAll_comOperadorDeAcervo_deveRetornar403ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE, tokenDoPerfil("warehouse_op"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody())
                .containsEntry("status", 403)
                .containsEntry("message", MSG_SEM_PERMISSAO);
    }

    // ----------------------------------------------------------------- GET /api/usuarios/{username}

    @Test
    void findById_comUsernameExistente_deveRetornar200ComDadosDoUsuario() {
        // Arrange
        Usuario novo = criarUsuario("loan_op");

        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/" + novo.username(), tokenAdmin());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("username", novo.username())
                .containsEntry("email", novo.email())
                .containsEntry("completeName", "Usuário " + novo.username())
                .containsEntry("role_code", "loan_op")
                .containsEntry("status", true)
                .doesNotContainKey("password");
        assertThat(comoLong(resposta.getBody().get("userId"))).isEqualTo(novo.id());
    }

    @Test
    void findById_comUsernameInexistente_deveRetornar404ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = get(BASE + "/naoexiste" + uid(), tokenAdmin());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("message", "Usuário não encontrado");
    }

    // ----------------------------------------------------------------- POST /api/usuarios

    @Test
    void create_comAdminDeUsuarios_deveRetornar200EGravarSenhaCriptografada() {
        // Arrange
        String id = uid();
        String username = "n" + id;

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, tokenAdmin(), corpoUsuario(username, username + "@teste.br", "warehouse_op"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("username", username)
                .containsEntry("role_code", "warehouse_op")
                .containsEntry("status", true)
                .doesNotContainKey("password");
        assertThat(resposta.getBody().get("userId")).isNotNull();

        UserModel gravado = userRepository.findByUsername(username);
        assertThat(gravado).isNotNull();
        assertThat(gravado.getPassword()).startsWith("$2").isNotEqualTo(SENHA_PADRAO);   // só o hash BCrypt
    }

    @Test
    void create_comUsernameDuplicado_deveRetornar409ComMensagem() {
        // Arrange
        Usuario existente = criarUsuario("loan_op");
        Map<String, Object> corpo = corpoUsuario(existente.username(), "outro" + uid() + "@teste.br", "loan_op");

        // Act
        ResponseEntity<Map<String, Object>> resposta = post(BASE, tokenAdmin(), corpo);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody())
                .containsEntry("status", 409)
                .containsEntry("message", MSG_INTEGRIDADE);
    }

    @Test
    void create_semToken_deveRetornar401SemGravarUsuario() {
        // Arrange
        String username = "s" + uid();

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE, null, corpoUsuario(username, username + "@teste.br", "root"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(userRepository.findByUsername(username)).isNull();
    }

    // ----------------------------------------------------------------- PUT /api/usuarios/{username}

    @Test
    void update_comDadosValidos_deveRetornar200EGravarAlteracoesMantendoSenha() {
        // Arrange
        Usuario usuario = criarUsuario("loan_op");
        String hashAntes = userRepository.findByUsername(usuario.username()).getPassword();
        String novoEmail = "e" + uid() + "@teste.br";
        Map<String, Object> corpo = Map.of(
                "completeName", "Nome Alterado",
                "email", novoEmail,
                "role_code", "warehouse_op",
                "password", "");

        // Act
        ResponseEntity<Map<String, Object>> resposta = put(BASE + "/" + usuario.username(), tokenAdmin(), corpo);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("completeName", "Nome Alterado")
                .containsEntry("email", novoEmail)
                .containsEntry("role_code", "warehouse_op");

        UserModel gravado = userRepository.findByUsername(usuario.username());
        assertThat(gravado.getCompleteName()).isEqualTo("Nome Alterado");
        assertThat(gravado.getRole_code()).isEqualTo("warehouse_op");
        assertThat(gravado.getPassword()).isEqualTo(hashAntes);       // senha em branco não troca a senha
    }

    @Test
    void update_comUsernameInexistente_deveRetornar404ComMensagem() {
        // Arrange
        Map<String, Object> corpo = Map.of("completeName", "X", "email", "x@teste.br", "role_code", "loan_op");

        // Act
        ResponseEntity<Map<String, Object>> resposta = put(BASE + "/naoexiste" + uid(), tokenAdmin(), corpo);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("message", "Usuário não encontrado");
    }

    // ----------------------------------------------------------------- PATCH /api/usuarios/{username}/status/...

    @Test
    void deactivate_comUsuarioAtivo_deveRetornar200EBloquearLogin() {
        // Arrange
        Usuario usuario = criarUsuario("loan_op");

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                patch(BASE + "/" + usuario.username() + "/status/desativar", tokenAdmin());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.findByUsername(usuario.username()).getStatus()).isFalse();   // não foi excluído
        assertThat(post("/api/auth/login", null, credenciais(usuario.username(), usuario.senha())).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deactivate_comUsernameInexistente_deveRetornar404ComMensagem() {
        // Act
        ResponseEntity<Map<String, Object>> resposta =
                patch(BASE + "/naoexiste" + uid() + "/status/desativar", tokenAdmin());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("message", "Usuário não encontrado");
    }

    @Test
    void activate_comUsuarioDesativado_deveRetornar200ELiberarLogin() {
        // Arrange
        Usuario usuario = criarUsuario("loan_op");
        patch(BASE + "/" + usuario.username() + "/status/desativar", tokenAdmin());

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                patch(BASE + "/" + usuario.username() + "/status/ativar", tokenAdmin());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.findByUsername(usuario.username()).getStatus()).isTrue();
        assertThat(login(usuario.username(), usuario.senha())).isNotBlank();
    }

    // ----------------------------------------------------------------- POST /api/usuarios/aluno

    @Test
    void createStudent_comOperadorDeEmprestimo_deveRetornar200ECriarUsuarioVinculadoAoAluno() {
        // Arrange
        String id = uid();
        String username = "a" + id;
        String ra = "RA" + id;

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE + "/aluno", tokenDoPerfil("loan_op"), corpoAluno(username, ra));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);

        UserModel usuario = userRepository.findByUsername(username);
        assertThat(usuario).isNotNull();
        assertThat(usuario.getRole_code()).isEqualTo("students");
        assertThat(usuario.getPassword()).startsWith("$2");

        StudentModel aluno = studentRepository.findByRa(ra);
        assertThat(aluno).isNotNull();
        assertThat(aluno.getUserId()).isEqualTo(usuario.getUserId());   // aluno ligado ao usuário criado
    }

    @Test
    void createStudent_comOperadorDeAcervo_deveRetornar403SemCriarAluno() {
        // Arrange
        String id = uid();
        String ra = "RA" + id;

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                post(BASE + "/aluno", tokenDoPerfil("warehouse_op"), corpoAluno("a" + id, ra));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("message", MSG_SEM_PERMISSAO);
        assertThat(studentRepository.findByRa(ra)).isNull();
    }
}
