package searchoteca.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.repository.LoginAttemptRepository;

import static sun.security.krb5.internal.ktab.KeyTab.normalize;

@Service
public class LoginAttemptService {
    private LoginAttemptRepository repository;
    private AuditLogService auditLogService;
    private final int maxAttempts;
    private final int lockMinutes;
    private final int windowMinutes;

    public LoginAttemptService(LoginAttemptRepository repository,
                               AuditLogService auditLogService,
                               @Value("${login-attempts.maxAttempts}") int maxAttempts,
                               @Value("${login-attempts.lockMinutes}") int lockMinutes,
                               @Value("${login-attempts.windowsMinutes}") int windowMinutes) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
        this.windowMinutes = windowMinutes;
    }
    // ... campos e construtor, com os valores do application.yaml (padrão 5 tentativas, 5 min, janela de 15 min)

    /** Antes de conferir a senha: se estiver bloqueado, recusa sem nem testar. */
    public void checkNotLocked(String rawUsername) {
        String username = normalize(rawUsername);
        if (username == null) return;

        repository.secondsLocked(username).ifPresent(seconds -> {
            auditLogService.save(username, AuditAction.LOGIN_BLOCKED, "tentativa durante o bloqueio");
            throw new LoginLockedException(seconds);
        });
    }

    /** Senha errada: conta a falha e, se chegou no limite, bloqueia. */
    public void registerFailure(String rawUsername) {
        String username = normalize(rawUsername);
        if (username == null) return;
        public class LoginLockedException extends RuntimeException {
            private final long retryAfterSeconds;

            public LoginLockedException(long retryAfterSeconds) {
                super(message(retryAfterSeconds));
                this.retryAfterSeconds = retryAfterSeconds;
            }

            private static String message(long seconds) {
                long minutes = Math.max(1, (seconds + 59) / 60);   // arredonda para cima: 61 s -> 2 min
                return "Muitas tentativas de login sem sucesso. Tente novamente em "
                        + minutes + (minutes == 1 ? " minuto." : " minutos.");
            }
        }
        int attempts = repository.registerFailure(username, windowMinutes);
        if (attempts >= maxAttempts) {
            repository.lock(username, lockMinutes);
            auditLogService.save(username, AuditAction.LOGIN_LOCKED,
                    attempts + " senhas erradas seguidas: bloqueado por " + lockMinutes + " min");
            throw new LoginLockedException(lockMinutes * 60L);
        }
    }

    /** Senha certa: esquece as falhas anteriores. */
    public void registerSuccess(String rawUsername) {
        String username = normalize(rawUsername);
        if (username == null) return;

        repository.clear(username);
    }
}

