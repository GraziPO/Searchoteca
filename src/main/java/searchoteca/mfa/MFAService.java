package searchoteca.mfa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import searchoteca.DTO.MfaChallengeResponse;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.security.CustomUserDetails;
import searchoteca.security.CustomUserDetailsService;
import searchoteca.security.JWTService;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class MFAService {

    private static final Logger log = LoggerFactory.getLogger(MFAService.class);

    private final SecureRandom random = new SecureRandom();

    private final CodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JWTService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuditLogService auditLogService;

    private final long codeTtlMinutes;
    private final int maxAttempts;
    private final long resendCooldownSeconds;
    private final int maxCodesPerHour;
    private final String mockCode;   // vazio = modo normal (código aleatório enviado por e-mail)

    public MFAService(CodeRepository codeRepository,
                      PasswordEncoder passwordEncoder,
                      EmailService emailService,
                      JWTService jwtService,
                      CustomUserDetailsService userDetailsService,
                      AuditLogService auditLogService,
                      @Value("${mfa.code-ttl-minutes:10}") long codeTtlMinutes,
                      @Value("${mfa.max-attempts:5}") int maxAttempts,
                      @Value("${mfa.resend-cooldown-seconds:60}") long resendCooldownSeconds,
                      @Value("${mfa.max-codes-per-hour:5}") int maxCodesPerHour,
                      @Value("${mfa.mock-code:}") String mockCode) {
        this.codeRepository = codeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.auditLogService = auditLogService;
        this.codeTtlMinutes = codeTtlMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxCodesPerHour = maxCodesPerHour;
        this.mockCode = mockCode == null ? "" : mockCode.trim();

        if (!this.mockCode.isEmpty()) {
            if (!this.mockCode.matches("\\d{6}")) {
                throw new IllegalStateException("mfa.mock-code (MFA_MOCK_CODE) deve ter exatamente 6 dígitos.");
            }
            log.warn("=================================================================");
            log.warn(" MFA EM MODO DE TESTE: nenhum e-mail será enviado e o código");
            log.warn(" aceito é o definido em MFA_MOCK_CODE. NÃO use em produção.");
            log.warn("=================================================================");
        }
    }

    /** Etapa 1: senha já conferida. Envia o código e devolve o mfaToken. */
    public MfaChallengeResponse startChallenge(CustomUserDetails user) {
        issueCode(user);
        return challengeResponse(user, jwtService.generateMFAToken(user));
    }

    /** Reenvio, respeitando o intervalo mínimo entre e-mails. */
    public MfaChallengeResponse resend(String mfaToken) {
        CustomUserDetails user = loadFromMfaToken(mfaToken);

        codeRepository.findLatest(user.getUserId()).ifPresent(last -> {
            long elapsed = Duration.between(last.getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (elapsed < resendCooldownSeconds) {
                throw MfaException.tooManyRequests(
                        "Aguarde " + (resendCooldownSeconds - elapsed) + " segundos para pedir um novo código.");
            }
        });

        issueCode(user);
        return challengeResponse(user, mfaToken);
    }

    /** Etapa 2: confere o código. Se estiver certo, devolve o usuário para gerar o JWT completo. */
    public CustomUserDetails verify(String mfaToken, String code) {
        CustomUserDetails user = loadFromMfaToken(mfaToken);

        if (code == null || !code.trim().matches("\\d{6}")) {
            throw MfaException.invalid("O código deve ter 6 dígitos.");
        }

        LocalDateTime now = LocalDateTime.now();
        CodeModel active = codeRepository.findActive(user.getUserId(), now)
                .orElseThrow(() -> MfaException.invalid("Código expirado ou já utilizado. Solicite um novo código."));

        if (!passwordEncoder.matches(code.trim(), active.getCodeHash())) {
            codeRepository.incrementAttempt(active.getId());
            int used = active.getAttempts() + 1;
            auditLogService.save(user.getUsername(), AuditAction.MFA_FAILURE,
                    "tentativa " + used + " de " + maxAttempts);

            if (used >= maxAttempts) {
                codeRepository.usedCode(active.getId(), now);
                auditLogService.save(user.getUsername(), AuditAction.MFA_LOCKED, "código invalidado por excesso de tentativas");
                throw MfaException.invalid("Número máximo de tentativas atingido. Faça login novamente.");
            }
            throw MfaException.invalid("Código incorreto. Restam " + (maxAttempts - used) + " tentativa(s).");
        }

        // Garante uso único mesmo com duas requisições simultâneas
        if (codeRepository.usedCode(active.getId(), now) == 0) {
            throw MfaException.invalid("Código já utilizado. Faça login novamente.");
        }

        auditLogService.save(user.getUsername(), AuditAction.MFA_SUCCESS, null);
        return user;
    }

    private void issueCode(CustomUserDetails user) {
        LocalDateTime now = LocalDateTime.now();

        long recent = codeRepository.countCreatedSince(user.getUserId(), now.minusHours(1));
        if (recent >= maxCodesPerHour) {
            auditLogService.save(user.getUsername(), AuditAction.MFA_LOCKED, "limite de códigos por hora");
            throw MfaException.tooManyRequests("Limite de códigos por hora atingido. Tente novamente mais tarde.");
        }

        codeRepository.invalidateAll(user.getUserId(), now);

        boolean mock = !mockCode.isEmpty();
        String code = mock ? mockCode : String.format("%06d", random.nextInt(1_000_000));

        CodeModel mfaCode = new CodeModel();
        mfaCode.setUserId(user.getUserId());
        mfaCode.setCodeHash(passwordEncoder.encode(code));   // só o hash vai para o banco
        mfaCode.setCreatedAt(now);
        mfaCode.setExpiresAt(now.plusMinutes(codeTtlMinutes));
        mfaCode.setAttempts(0);
        codeRepository.save(mfaCode);

        if (mock) {
            // Modo de teste: o resto do fluxo (hash, validade, tentativas, uso único) continua igual
            auditLogService.save(user.getUsername(), AuditAction.MFA_CODE_SENT, "modo de teste: e-mail não enviado");
            return;
        }

        emailService.sendCode(user.getEmail(), user.getCompleteName(), code, codeTtlMinutes);

        // Nunca registrar o código em log ou auditoria
        auditLogService.save(user.getUsername(), AuditAction.MFA_CODE_SENT,
                "enviado para " + EmailMasker.mask(user.getEmail()));
    }

    private CustomUserDetails loadFromMfaToken(String mfaToken) {
        String invalid = "Verificação expirada ou inválida. Faça login novamente.";
        if (mfaToken == null || mfaToken.isBlank()) {
            throw MfaException.invalid(invalid);
        }
        try {
            String username = jwtService.extractMfaUsername(mfaToken);
            CustomUserDetails user = (CustomUserDetails) userDetailsService.loadUserByUsername(username);
            if (!user.isEnabled()) {
                throw MfaException.invalid(invalid);
            }
            return user;
        } catch (MfaException e) {
            throw e;
        } catch (UsernameNotFoundException | io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw MfaException.invalid(invalid);
        }
    }

    private MfaChallengeResponse challengeResponse(CustomUserDetails user, String mfaToken) {
        return new MfaChallengeResponse(true, mfaToken, EmailMasker.mask(user.getEmail()), codeTtlMinutes * 60);
    }
}
