package searchoteca.resetPassword;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.mfa.EmailMasker;
import searchoteca.mfa.EmailService;
import searchoteca.mfa.MfaException;
import searchoteca.model.UserModel;
import searchoteca.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {
    private static final String INVALID = "Código inválido ou expirado.";

    private final SecureRandom random = new SecureRandom();
    private final ResetCodeRepository resetCodeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    private final long codeTtlMinutes;
    private final int maxAttempts;
    private final long cooldownSeconds;
    private final int maxCodesPerHour;

    public PasswordResetService(ResetCodeRepository resetCodeRepository, UserRepository userRepository,
                                PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
                                EmailService emailService, AuditLogService auditLogService,
                                @Value("${password-reset.code-ttl-minutes:15}") long codeTtlMinutes,
                                @Value("${password-reset.max-attempts:5}") int maxAttempts,
                                @Value("${password-reset.cooldown-seconds:60}") long cooldownSeconds,
                                @Value("${password-reset.max-codes-per-hour:5}") int maxCodesPerHour) {
        this.resetCodeRepository = resetCodeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.codeTtlMinutes = codeTtlMinutes;
        this.maxAttempts = maxAttempts;
        this.cooldownSeconds = cooldownSeconds;
        this.maxCodesPerHour = maxCodesPerHour;
    }

    public void requestReset(String login) {
        UserModel user = findByLogin(login);
        if (user == null || !Boolean.TRUE.equals(user.getStatus())) {
            auditLogService.save(login, AuditAction.PASSWORD_RESET_REQUESTED, "usuário bloqueado, inexistente ou inativo");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Long userId = user.getUserId();

        //Bloqueia novos pedidos quando atinge  o limite de requisições, silenciosamente
        boolean tooSoon = resetCodeRepository.findLatest(userId)
                .map(last -> Duration.between(last.getCreatedAt(), now).getSeconds() < cooldownSeconds)
                .orElse(false);
        if (tooSoon || resetCodeRepository.countCreatedSince(userId, now.minusHours(1)) >= maxCodesPerHour) {
            auditLogService.save(user.getUsername(), AuditAction.PASSWORD_RESET_REQUESTED, "pedido ignorado: limite de envios");
            return;
        }

        resetCodeRepository.invalidateAll(userId, now);

        String code = String.format("%06d", random.nextInt(1_000_000));
        ResetCodeModel reset = new ResetCodeModel();
        reset.setUserId(userId);
        reset.setCodeHash(passwordEncoder.encode(code));
        reset.setCreatedAt(now);
        reset.setExpiresAt(now.plusMinutes(codeTtlMinutes));
        reset.setAttempts(0);
        resetCodeRepository.save(reset);

        emailService.sendPasswordResetCode(user.getEmail(), user.getCompleteName(), code, codeTtlMinutes);
        auditLogService.save(user.getUsername(), AuditAction.PASSWORD_RESET_REQUESTED,
                "código enviado para " + EmailMasker.mask(user.getEmail()));
    }

    @Transactional
    public void resetPassword(String login, String code, String newPassword) {
        passwordPolicy.validatePassword(newPassword);

        UserModel user = findByLogin(login);
        if (user == null || !Boolean.TRUE.equals(user.getStatus())
                || code == null || !code.trim().matches("\\d{6}")) {
            throw MfaException.invalid(INVALID);
        }

        LocalDateTime now = LocalDateTime.now();

        ResetCodeModel active = resetCodeRepository.findActive(user.getUserId(), now)
                .orElseThrow(() -> MfaException.invalid(INVALID));

        if (!passwordEncoder.matches(code.trim(), active.getCodeHash())) {
            resetCodeRepository.incrementAttempt(active.getId());
            if (active.getAttempts() + 1 >= maxAttempts) {
                resetCodeRepository.usedCode(active.getId(), now);   // código queimado
            }
            auditLogService.save(user.getUsername(), AuditAction.PASSWORD_RESET_FAILURE,
                    "tentativa " + (active.getAttempts() + 1) + " de " + maxAttempts);
            throw MfaException.invalid(INVALID);
        }


        if (resetCodeRepository.usedCode(active.getId(), now) == 0) {
            throw MfaException.invalid(INVALID);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.save(user.getUsername(), AuditAction.PASSWORD_RESET_SUCCESS, null);
    }

    private UserModel findByLogin(String login) {
        if (login == null || login.isBlank()) return null;
        String value = login.trim();
        return value.contains("@") ? userRepository.findByEmail(value) : userRepository.findByUsername(value);
    }
}