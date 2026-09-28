package searchoteca.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JWTService {
    // Tokens da etapa 1 do login carregam stage=MFA_PENDING e NÃO dão acesso à API
    private static final String STAGE_CLAIM = "stage";
    private static final String STAGE_MFA_PENDING = "MFA_PENDING";
    private static final String STAGE_TERMS_PENDING = "TERMS_PENDING";

    private final SecretKey secret;
    private final long TokenTTl;
    private final long mfaTokenTtl;
    private final long termsTokenTtl;

    public JWTService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs,
            @Value("${mfa.token-ttl-ms:900000}") long mfaTokenTtlMs,
            @Value("${legal.token-ttl-ms:900000}") long termsTokenTtlMs){

        this.secret = Keys.hmacShaKeyFor(secret.getBytes());
        this.TokenTTl = expirationMs;
        this.mfaTokenTtl = mfaTokenTtlMs;
        this.termsTokenTtl = termsTokenTtlMs;
    }

    public String generateToken(CustomUserDetails userDetails) {
        Date currentDate = new Date();
        Date expiryDate = new Date(currentDate.getTime() + TokenTTl);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId",userDetails.getUserId())
                .claim("role_code", userDetails.getRoleCode())
                .issuedAt(currentDate)
                .expiration(expiryDate)
                .signWith(secret)
                .compact();
    }

    /** Token curto da etapa 1: só serve para /api/auth/mfa/verify e /resend. */
    public String generateMFAToken(CustomUserDetails userDetails) {
        Date currentDate = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(STAGE_CLAIM, STAGE_MFA_PENDING)
                .issuedAt(currentDate)
                .expiration(new Date(currentDate.getTime() + mfaTokenTtl))
                .signWith(secret)
                .compact();
    }

    /** Valida assinatura, expiração e se é mesmo um token de MFA. Lança JwtException se não for. */
    public String extractMfaUsername(String token) {
        Claims claims = parseClaims(token);
        if (!STAGE_MFA_PENDING.equals(claims.get(STAGE_CLAIM, String.class))) {
            throw new JwtException("Token não é de verificação MFA");
        }
        return claims.getSubject();
    }

    /** Token curto de "senha conferida, falta aceitar os termos": só serve para /api/auth/terms/accept. */
    public String generateTermsToken(CustomUserDetails userDetails) {
        Date currentDate = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(STAGE_CLAIM, STAGE_TERMS_PENDING)
                .issuedAt(currentDate)
                .expiration(new Date(currentDate.getTime() + termsTokenTtl))
                .signWith(secret)
                .compact();
    }

    public String extractTermsUsername(String token) {
        Claims claims = parseClaims(token);
        if (!STAGE_TERMS_PENDING.equals(claims.get(STAGE_CLAIM, String.class))) {
            throw new JwtException("Token não é de aceite de termos");
        }
        return claims.getSubject();
    }

    public String extractUsername(String token){
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, CustomUserDetails userDetails){
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(userDetails.getUsername())
                && !claims.getExpiration().before(new Date())
                && claims.get(STAGE_CLAIM) == null;   // recusa o mfaToken nas rotas protegidas
    }

    public boolean isTokenExpired(String token){
        return parseClaims(token).getExpiration().before(new Date());
    }

    public Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token).getPayload();
    }
}
