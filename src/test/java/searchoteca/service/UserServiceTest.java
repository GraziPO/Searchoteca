package searchoteca.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import searchoteca.model.UserModel;
import searchoteca.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RF12 e regras de "Usuários e Segurança": senha armazenada apenas como hash,
 * usuário criado ativo e desativação sem exclusão dos dados.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private UserModel usuario(String username, String senha, Boolean status) {
        UserModel u = new UserModel();
        u.setUsername(username);
        u.setCompleteName("João da Silva");
        u.setEmail("joao@umc.br");
        u.setRole_code("OPERADOR");
        u.setPassword(senha);
        u.setStatus(status);
        return u;
    }

    @Test
    void deveCriptografarSenhaEAtivarUsuarioAoCriar() {
        // Arrange - mesmo que o status venha falso no cadastro, o usuário nasce ativo
        UserModel novo = usuario("joao", "Senha@123", false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("$2a$10$hashDaSenha");
        when(userRepository.save(any(UserModel.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserModel criado = userService.create(novo);

        // Assert
        assertEquals("$2a$10$hashDaSenha", criado.getPassword());
        assertNotEquals("Senha@123", criado.getPassword());
        assertTrue(criado.getStatus());
    }

    @Test
    void deveCriptografarNovaSenhaQuandoInformadaNaAtualizacao() {
        // Arrange
        UserModel existente = usuario("joao", "$2a$10$hashAntigo", true);
        when(userRepository.findByUsername("joao")).thenReturn(existente);
        when(passwordEncoder.encode("NovaSenha@1")).thenReturn("$2a$10$hashNovo");
        when(userRepository.save(any(UserModel.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserModel atualizado = userService.update("joao", usuario("joao", "NovaSenha@1", true));

        // Assert
        assertEquals("$2a$10$hashNovo", atualizado.getPassword());
        verify(passwordEncoder).encode("NovaSenha@1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveManterSenhaAtualQuandoNovaSenhaVierVaziaOuEmBranco(String senhaInformada) {
        // Arrange
        UserModel existente = usuario("joao", "$2a$10$hashAntigo", true);
        when(userRepository.findByUsername("joao")).thenReturn(existente);
        when(userRepository.save(any(UserModel.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserModel atualizado = userService.update("joao", usuario("joao", senhaInformada, true));

        // Assert
        assertEquals("$2a$10$hashAntigo", atualizado.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void deveIgnorarTentativaDeTrocarUsernamePeloCorpoDaAtualizacao() {
        // Arrange - usuários são únicos: o username vem da rota, não do corpo
        UserModel existente = usuario("joao", "$2a$10$hashAntigo", true);
        UserModel corpo = usuario("maria", null, true);
        corpo.setEmail("joao.silva@umc.br");
        when(userRepository.findByUsername("joao")).thenReturn(existente);
        when(userRepository.save(any(UserModel.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserModel atualizado = userService.update("joao", corpo);

        // Assert
        assertEquals("joao", atualizado.getUsername());
        assertEquals("joao.silva@umc.br", atualizado.getEmail());
    }

    @Test
    void deveDesativarUsuarioSemExcluirSeusDados() {
        // Arrange
        UserModel existente = usuario("joao", "$2a$10$hashAntigo", true);
        when(userRepository.findByUsername("joao")).thenReturn(existente);

        // Act
        userService.deactivate("joao");

        // Assert
        assertFalse(existente.getStatus());
        verify(userRepository).save(existente);
        verify(userRepository, never()).delete(any(UserModel.class));
        verify(userRepository, never()).deleteByUsername(any());
    }
}
