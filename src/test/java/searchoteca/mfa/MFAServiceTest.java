package searchoteca.mfa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import searchoteca.DTO.MfaChallengeResponse;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.model.UserModel;
import searchoteca.security.CustomUserDetails;
import searchoteca.security.CustomUserDetailsService;
import searchoteca.security.JWTService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RF12 / RF13 - Verificação multifator por e-mail: código de 6 dígitos,
 * validade, limite de tentativas, uso único, limite de envios e reenvio.
 */
@ExtendWith(MockitoExtension.class)
class MFAServiceTest {

    private static final String MFA_TOKEN = "token-mfa-valido";
    private static final String HASH = "$2a$10$hashDoCodigo";
    private static final long TTL_MINUTOS = 10;
    private static final int MAX_TENTATIVAS = 5;
    private static final long INTERVALO_REENVIO_SEG = 60;
    private static final int MAX_CODIGOS_POR_HORA = 5;

    @Mock CodeRepository codeRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @Mock JWTService jwtService;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock AuditLogService auditLogService;

    MFAService mfaService;
    CustomUserDetails usuario;

    @BeforeEach
    void setUp() {
        mfaService = novoServico("");
        usuario = usuarioComStatus(true);
    }

    private MFAService novoServico(String mockCode) {
        return new MFAService(codeRepository, passwordEncoder, emailService, jwtService,
                userDetailsService, auditLogService,
                TTL_MINUTOS, MAX_TENTATIVAS, INTERVALO_REENVIO_SEG, MAX_CODIGOS_POR_HORA, mockCode);
    }

    private CustomUserDetails usuarioComStatus(boolean ativo) {
        UserModel u = new UserModel();
        u.setId(1L);
        u.setUsername("joao");
        u.setCompleteName("João da Silva");
        u.setEmail("joao@umc.br");
        u.setRole_code("OPERADOR");
        u.setStatus(ativo);
        return new CustomUserDetails(u, List.of(), false);
    }

    private void tokenValidoPara(CustomUserDetails user) {
        when(jwtService.extractMfaUsername(MFA_TOKEN)).thenReturn("joao");
        when(userDetailsService.loadUserByUsername("joao")).thenReturn(user);
    }

    private CodeModel codigoAtivo(int tentativasJaUsadas) {
        LocalDateTime agora = LocalDateTime.now();
        CodeModel c = new CodeModel();
        c.setId(10L);
        c.setUserId(1L);
        c.setCodeHash(HASH);
        c.setAttempts(tentativasJaUsadas);
        c.setCreatedAt(agora.minusMinutes(1));
        c.setExpiresAt(agora.plusMinutes(TTL_MINUTOS - 1));
        return c;
    }

    // ===================== verify() =====================

    @Test
    void deveAutenticarQuandoCodigoCorretoEAindaValido() {
        // Arrange
        tokenValidoPara(usuario);
        when(codeRepository.findActive(eq(1L), any(LocalDateTime.class))).thenReturn(Optional.of(codigoAtivo(0)));
        when(passwordEncoder.matches("123456", HASH)).thenReturn(true);
        when(codeRepository.usedCode(eq(10L), any(LocalDateTime.class))).thenReturn(1);

        // Act
        CustomUserDetails autenticado = mfaService.verify(MFA_TOKEN, "123456");

        // Assert
        assertSame(usuario, autenticado);
        verify(auditLogService).save("joao", AuditAction.MFA_SUCCESS, null);
        verify(codeRepository, never()).incrementAttempt(anyLong());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "1234567", "12a456", "      "})
    void deveRecusarCodigoComFormatoInvalidoSemConsultarCodigosSalvos(String codigoDigitado) {
        // Arrange
        tokenValidoPara(usuario);

        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.verify(MFA_TOKEN, codigoDigitado));

        // Assert
        assertEquals("O código deve ter 6 dígitos.", ex.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verifyNoInteractions(codeRepository, passwordEncoder);
    }

    @Test
    void deveIncrementarTentativaEInformarTentativasRestantesQuandoCodigoIncorreto() {
        // Arrange
        tokenValidoPara(usuario);
        when(codeRepository.findActive(eq(1L), any(LocalDateTime.class))).thenReturn(Optional.of(codigoAtivo(0)));
        when(passwordEncoder.matches("654321", HASH)).thenReturn(false);

        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.verify(MFA_TOKEN, "654321"));

        // Assert
        assertEquals("Código incorreto. Restam 4 tentativa(s).", ex.getMessage());
        verify(codeRepository).incrementAttempt(10L);
        verify(auditLogService).save("joao", AuditAction.MFA_FAILURE, "tentativa 1 de 5");
        verify(codeRepository, never()).usedCode(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void deveInvalidarCodigoQuandoAtingeExatamenteOLimiteDeTentativas() {
        // Arrange - já houve 4 erros; este é o 5º (limite configurado)
        tokenValidoPara(usuario);
        when(codeRepository.findActive(eq(1L), any(LocalDateTime.class))).thenReturn(Optional.of(codigoAtivo(4)));
        when(passwordEncoder.matches("654321", HASH)).thenReturn(false);

        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.verify(MFA_TOKEN, "654321"));

        // Assert
        assertEquals("Número máximo de tentativas atingido. Faça login novamente.", ex.getMessage());
        verify(codeRepository).usedCode(eq(10L), any(LocalDateTime.class));
        verify(auditLogService).save("joao", AuditAction.MFA_LOCKED, "código invalidado por excesso de tentativas");
    }

    @Test
    void deveRecusarCodigoCorretoQuandoJaFoiUsadoPorRequisicaoSimultanea() {
        // Arrange - outro pedido marcou o código como usado primeiro (UPDATE retornou 0 linhas)
        tokenValidoPara(usuario);
        when(codeRepository.findActive(eq(1L), any(LocalDateTime.class))).thenReturn(Optional.of(codigoAtivo(0)));
        when(passwordEncoder.matches("123456", HASH)).thenReturn(true);
        when(codeRepository.usedCode(eq(10L), any(LocalDateTime.class))).thenReturn(0);

        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.verify(MFA_TOKEN, "123456"));

        // Assert
        assertEquals("Código já utilizado. Faça login novamente.", ex.getMessage());
        verify(auditLogService, never()).save(any(), eq(AuditAction.MFA_SUCCESS), any());
    }

    @Test
    void deveRecusarQuandoNaoHaCodigoAtivoPorExpiracaoOuUsoAnterior() {
        // Arrange
        tokenValidoPara(usuario);
        when(codeRepository.findActive(eq(1L), any(LocalDateTime.class))).thenReturn(Optional.empty());

        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.verify(MFA_TOKEN, "123456"));

        // Assert
        assertEquals("Código expirado ou já utilizado. Solicite um novo código.", ex.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void deveRecusarVerificacaoDeUsuarioDesativado() {
        // Arrange
        tokenValidoPara(usuarioComStatus(false));

        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.verify(MFA_TOKEN, "123456"));

        // Assert
        assertEquals("Verificação expirada ou inválida. Faça login novamente.", ex.getMessage());
        verifyNoInteractions(codeRepository);
    }

    @Test
    void deveRecusarTokenMfaEmBrancoSemDecodificarJwt() {
        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.verify("   ", "123456"));

        // Assert
        assertEquals("Verificação expirada ou inválida. Faça login novamente.", ex.getMessage());
        verifyNoInteractions(jwtService, codeRepository);
    }

    // ===================== startChallenge() / resend() =====================

    @Test
    void deveGerarCodigoDeSeisDigitosSalvarApenasOHashEEnviarPorEmail() {
        // Arrange
        when(codeRepository.countCreatedSince(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn(HASH);
        when(jwtService.generateMFAToken(usuario)).thenReturn(MFA_TOKEN);

        // Act
        MfaChallengeResponse resposta = mfaService.startChallenge(usuario);

        // Assert - resposta ao cliente com e-mail mascarado
        assertTrue(resposta.mfaRequired());
        assertEquals(MFA_TOKEN, resposta.mfaToken());
        assertEquals("j**o@umc.br", resposta.email());
        assertEquals(TTL_MINUTOS * 60, resposta.expiresInSeconds());

        // Assert - código enviado tem 6 dígitos e é o mesmo que foi transformado em hash
        ArgumentCaptor<String> codigoEnviado = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendCode(eq("joao@umc.br"), eq("João da Silva"), codigoEnviado.capture(), eq(TTL_MINUTOS));
        assertTrue(codigoEnviado.getValue().matches("\\d{6}"));
        verify(passwordEncoder).encode(codigoEnviado.getValue());

        // Assert - banco recebe só o hash, com validade e tentativas zeradas
        ArgumentCaptor<CodeModel> salvo = ArgumentCaptor.forClass(CodeModel.class);
        verify(codeRepository).save(salvo.capture());
        assertEquals(HASH, salvo.getValue().getCodeHash());
        assertEquals(0, salvo.getValue().getAttempts());
        assertEquals(salvo.getValue().getCreatedAt().plusMinutes(TTL_MINUTOS), salvo.getValue().getExpiresAt());
        verify(codeRepository).invalidateAll(eq(1L), any(LocalDateTime.class));

        // Assert - auditoria nunca registra o código, só o e-mail mascarado
        verify(auditLogService).save("joao", AuditAction.MFA_CODE_SENT, "enviado para j**o@umc.br");
    }

    @Test
    void deveBloquearNovoCodigoQuandoLimitePorHoraForExatamenteAtingido() {
        // Arrange - já foram gerados 5 códigos na última hora (limite configurado)
        when(codeRepository.countCreatedSince(eq(1L), any(LocalDateTime.class))).thenReturn(5L);

        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.startChallenge(usuario));

        // Assert
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertEquals("Limite de códigos por hora atingido. Tente novamente mais tarde.", ex.getMessage());
        verify(codeRepository, never()).save(any(CodeModel.class));
        verifyNoInteractions(emailService, jwtService);
    }

    @Test
    void deveRecusarReenvioAntesDoIntervaloMinimoEntreEmails() {
        // Arrange - último código foi criado há 30 segundos (intervalo mínimo: 60)
        tokenValidoPara(usuario);
        CodeModel ultimo = codigoAtivo(0);
        ultimo.setCreatedAt(LocalDateTime.now().minusSeconds(30));
        when(codeRepository.findLatest(1L)).thenReturn(Optional.of(ultimo));

        // Act
        MfaException ex = assertThrows(MfaException.class, () -> mfaService.resend(MFA_TOKEN));

        // Assert
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertTrue(ex.getMessage().startsWith("Aguarde "));
        verify(codeRepository, never()).countCreatedSince(anyLong(), any(LocalDateTime.class));
        verifyNoInteractions(emailService);
    }

    // ===================== modo de teste (MFA_MOCK_CODE) =====================

    @Test
    void naoDeveEnviarEmailQuandoModoDeTesteEstiverAtivo() {
        // Arrange
        MFAService servicoModoTeste = novoServico("123456");
        when(codeRepository.countCreatedSince(eq(1L), any(LocalDateTime.class))).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn(HASH);
        when(jwtService.generateMFAToken(usuario)).thenReturn(MFA_TOKEN);

        // Act
        servicoModoTeste.startChallenge(usuario);

        // Assert
        verifyNoInteractions(emailService);
        verify(auditLogService).save("joao", AuditAction.MFA_CODE_SENT, "modo de teste: e-mail não enviado");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "abcdef"})
    void deveImpedirInicializacaoQuandoCodigoDeTesteNaoTemSeisDigitos(String codigoInvalido) {
        // Act
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> novoServico(codigoInvalido));

        // Assert
        assertTrue(ex.getMessage().contains("exatamente 6 dígitos"));
    }
}
