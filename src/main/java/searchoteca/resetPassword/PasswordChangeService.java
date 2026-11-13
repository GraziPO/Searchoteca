package searchoteca.resetPassword;

import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.mfa.MfaException;
import searchoteca.model.UserModel;
import searchoteca.repository.users.UserRepository;
import searchoteca.security.CustomUserDetails;
import searchoteca.security.CustomUserDetailsService;
import searchoteca.security.JWTService;

import java.time.LocalDateTime;

@Service
public class PasswordChangeService {
    private static final String SESSION_INVALID = "Sessão expirada ou inválida. Faça login novamente.";

    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AuditLogService auditLogService;

    public PasswordChangeService(JWTService jwtService,
                                 UserRepository userRepository,
                                 CustomUserDetailsService userDetailsService,
                                 PasswordEncoder passwordEncoder,
                                 PasswordPolicy passwordPolicy,
                                 AuditLogService auditLogService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.auditLogService = auditLogService;
    }

    /** Etapa final do primeiro acesso: troca a senha e devolve o usuário para emitir o token completo. */
    @Transactional
    public CustomUserDetails completeFirstAccess(String token, String newPassword) {
        String username;
        try {
            username = jwtService.extractPasswordChangeToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw MfaException.invalid(SESSION_INVALID);
        }

        UserModel user = userRepository.findByUsername(username);
        // A flag já desligada significa que este token foi usado: impede reaproveitá-lo
        if (user == null || !Boolean.TRUE.equals(user.getStatus())
                || !Boolean.TRUE.equals(user.getMust_change_password())) {
            throw MfaException.invalid(SESSION_INVALID);
        }

        applyNewPassword(user, newPassword);
        auditLogService.save(username, AuditAction.PASSWORD_CHANGED, "troca obrigatória no primeiro acesso");
        return (CustomUserDetails) userDetailsService.loadUserByUsername(username);
    }

    /** Troca voluntária, com o usuário já logado. */
    @Transactional
    public void changeOwnPassword(Long userId, String currentPassword, String newPassword) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResourceConflictException("A senha atual está incorreta");
        }
        applyNewPassword(user, newPassword);
        auditLogService.record(AuditAction.PASSWORD_CHANGED, "troca voluntária");
    }

    private void applyNewPassword(UserModel user, String newPassword) {
        passwordPolicy.validatePassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ResourceConflictException("A nova senha deve ser diferente da atual");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMust_change_password(false);
        user.setPasswordChangedAt(LocalDateTime.now());   // derruba os tokens emitidos antes (RF18)
        userRepository.save(user);
    }
}
