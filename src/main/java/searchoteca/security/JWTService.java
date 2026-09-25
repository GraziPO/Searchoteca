package searchoteca.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JWTService {
    private final SecretKey secret;
    private final long TokenTTl;

    public JWTService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs){

        this.secret = Keys.hmacShaKeyFor(secret.getBytes());
        this.TokenTTl = expirationMs;
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

    public String extractUsername(String token){
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, CustomUserDetails userDetails){
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
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
