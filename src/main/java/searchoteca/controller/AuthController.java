package searchoteca.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchoteca.DTO.*;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.legal.TermsService;
import searchoteca.mfa.MFAService;
import searchoteca.resetPassword.PasswordResetService;
import searchoteca.security.CustomUserDetails;
import searchoteca.security.JWTService;

import java.util.Map;

/**
 * Login em três etapas:
 *   1. /login           senha  -> (termos pendentes?) termsToken
 *   2. /termos/accept    aceite -> mfaToken (código enviado por e-mail)
 *   3. /mfa/verify      código -> JWT de acesso
 * Se os termos já foram aceitos, a etapa 2 é pulada. Se o MFA estiver desligado, a 3 também.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final AuditLogService auditLogService;
    private final MFAService mfaService;
    private final TermsService termsService;
    private final boolean mfaEnabled;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthenticationManager authenticationManager,
                          JWTService jwtService,
                          AuditLogService auditLogService,
                          MFAService mfaService,
                          TermsService termsService,
                          @Value("${mfa.enabled:true}") boolean mfaEnabled,
                          PasswordResetService passwordResetService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.mfaService = mfaService;
        this.termsService = termsService;
        this.mfaEnabled = mfaEnabled;
        this.passwordResetService = passwordResetService;
    }

    /** Etapa 1: confere usuário e senha. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
            );
        } catch (AuthenticationException ex) {
            auditLogService.save(loginRequest.username(), AuditAction.LOGIN_FAILURE, null);
            throw ex; // GlobalExceptionHandler -> 401
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Sem aceite da versão atual: para aqui. Nenhum código de MFA é enviado.
        if (!termsService.hasAcceptedCurrent(userDetails)) {
            return ResponseEntity.ok(termsService.challenge(userDetails));
        }
        return ResponseEntity.ok(continueLogin(userDetails));
    }

    /** Etapa 2: registra o aceite dos Termos e da Política e segue para o MFA. */
    @PostMapping("/termos/accept")
    public ResponseEntity<?> acceptTerms(@RequestBody TermsAcceptRequest request) {
        CustomUserDetails userDetails = termsService.accept(
                request.termsToken(), request.termsVersion(), request.privacyVersion());
        return ResponseEntity.ok(continueLogin(userDetails));
    }

    /** Etapa 3: confere o código do e-mail e devolve o JWT de acesso. */
    @PostMapping("/mfa/verify")
    public ResponseEntity<LoginResponse> verifyMFAToken(@RequestBody MfaVerifyRequest request) {
        CustomUserDetails userDetails = mfaService.verify(request.mfaToken(), request.code());
        return ResponseEntity.ok(issueFullToken(userDetails));
    }

    /** Gera e envia um novo código (o anterior deixa de valer). */
    @PostMapping("/mfa/resend")
    public ResponseEntity<MfaChallengeResponse> resendMFAToken(@RequestBody MfaResendRequest request) {
        return ResponseEntity.ok(mfaService.resend(request.mfaToken()));
    }

    /** Depois da senha e do aceite: envia o código do MFA ou, se estiver desligado, já libera o acesso. */
    private Object continueLogin(CustomUserDetails userDetails) {
        if (mfaEnabled) {
            return mfaService.startChallenge(userDetails);
        }
        return issueFullToken(userDetails);
    }

    private LoginResponse issueFullToken(CustomUserDetails userDetails) {
        String token = jwtService.generateToken(userDetails);
        auditLogService.recordLogin(userDetails.getUsername());
        return new LoginResponse(token, userDetails.getUsername(), userDetails.getRoleCode());
    }

    //========================= ESQUECI A SENHA =========================
    @PostMapping("/senha/esqueci")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.login());
        return ResponseEntity.ok(Map.of("message",
                "Se o usuário existir, Enviaremos um código para o e-mail cadastrado"));
    }

    @PostMapping("/senha/redefinir")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.login(),request.code(),request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
