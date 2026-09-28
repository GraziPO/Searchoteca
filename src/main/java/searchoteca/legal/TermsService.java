package searchoteca.legal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import searchoteca.DTO.LegalDocumentResponse;
import searchoteca.DTO.TermsChallengeResponse;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.security.CustomUserDetails;
import searchoteca.security.CustomUserDetailsService;
import searchoteca.security.JWTService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class TermsService {

    private final TermsAcceptanceRepository acceptanceRepository;
    private final JWTService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuditLogService auditLogService;

    private final String termsVersion;
    private final String privacyVersion;
    private final String termsContent;
    private final String privacyContent;

    public TermsService(TermsAcceptanceRepository acceptanceRepository,
                        JWTService jwtService,
                        CustomUserDetailsService userDetailsService,
                        AuditLogService auditLogService,
                        @Value("${legal.terms-version}") String termsVersion,
                        @Value("${legal.privacy-version}") String privacyVersion) {
        this.acceptanceRepository = acceptanceRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.auditLogService = auditLogService;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
        // Carregados uma vez na inicialização; se o arquivo faltar, a aplicação nem sobe
        this.termsContent = readResource("legal/termos-de-uso.md");
        this.privacyContent = readResource("legal/politica-de-privacidade.md");
    }

    /** O usuário já aceitou as versões atuais dos dois documentos? */
    public boolean hasAcceptedCurrent(CustomUserDetails user) {
        return acceptanceRepository.countAccepted(user.getUserId(), termsVersion, privacyVersion) > 0;
    }

    /** Senha conferida, mas falta o aceite: devolve o termsToken (não dá acesso à API nem dispara o MFA). */
    public TermsChallengeResponse challenge(CustomUserDetails user) {
        return new TermsChallengeResponse(true, jwtService.generateTermsToken(user), termsVersion, privacyVersion);
    }

    /** Registra o aceite e devolve o usuário para o login seguir (MFA ou JWT). */
    public CustomUserDetails accept(String termsToken, String acceptedTerms, String acceptedPrivacy) {
        CustomUserDetails user = loadFromTermsToken(termsToken);

        if (!termsVersion.equals(acceptedTerms) || !privacyVersion.equals(acceptedPrivacy)) {
            throw TermsException.outdated(
                    "Os documentos foram atualizados. Leia a versão atual e aceite novamente.");
        }

        if (!hasAcceptedCurrent(user)) {
            TermsAcceptanceModel acceptance = new TermsAcceptanceModel();
            acceptance.setUserId(user.getUserId());
            acceptance.setTermsVersion(termsVersion);
            acceptance.setPrivacyVersion(privacyVersion);
            acceptance.setAcceptedAt(LocalDateTime.now());
            try {
                acceptanceRepository.save(acceptance);
            } catch (DataIntegrityViolationException e) {
                // Duplo clique: outro pedido gravou o mesmo aceite no meio do caminho. Não é erro.
            }
            auditLogService.save(user.getUsername(), AuditAction.TERMS_ACCEPTED,
                    "termos v" + termsVersion + "; política de privacidade v" + privacyVersion);
        }
        return user;
    }

    public LegalDocumentResponse terms() {
        return new LegalDocumentResponse("Termos de Uso", termsVersion, termsContent);
    }

    public LegalDocumentResponse privacy() {
        return new LegalDocumentResponse("Política de Privacidade", privacyVersion, privacyContent);
    }

    private CustomUserDetails loadFromTermsToken(String termsToken) {
        String invalid = "Sessão de aceite expirada ou inválida. Faça login novamente.";
        if (termsToken == null || termsToken.isBlank()) {
            throw TermsException.invalid(invalid);
        }
        CustomUserDetails user;
        try {
            String username = jwtService.extractTermsUsername(termsToken);
            user = (CustomUserDetails) userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException | io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw TermsException.invalid(invalid);
        }
        if (!user.isEnabled()) {
            throw TermsException.invalid(invalid);
        }
        return user;
    }

    private static String readResource(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Documento legal não encontrado: " + path, e);
        }
    }
}
