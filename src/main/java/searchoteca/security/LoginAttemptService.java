package searchoteca.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.exception.LoginLockedException;
import searchoteca.repository.LoginAttemptRepository;

/**
 * Bloqueio temporário de login (RNF05).
 * Depois de max-attempts senhas erradas seguidas (padrão 5), o usuário fica bloqueado
 * por lock-minutes (padrão 5). Durante o bloqueio, nem a senha certa entra.
 */
@Service
public class LoginAttemptService {

    private final LoginAttemptRepository repository;
    private final AuditLogService auditLogService;
    private final int maxAttempts;
    private final int lockMinutes;
    private final int windowMinutes;

    public LoginAttemptService(LoginAttemptRepository repository,
                               AuditLogService auditLogService,
                               @Value("${login-lock.max-attempts:5}") int maxAttempts,
                               @Value("${login-lock.lock-minutes:5}") int lockMinutes,
                               @Value("${login-lock.window-minutes:15}") int windowMinutes) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
        this.windowMinutes = windowMinutes;
    }

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

    /** Desbloqueio antes do prazo, por um administrador. */
    public void unlock(String rawUsername) {
        String username = normalize(rawUsername);
        if (username == null) return;

        repository.clear(username);
    }

    /** Mesmo tratamento do AuditLogService: sem espaços nas pontas e no tamanho da coluna (30). */
    private static String normalize(String username) {
        if (username == null || username.isBlank()) return null;
        String u = username.trim();
        return u.length() > 30 ? u.substring(0, 30) : u;
    }
}